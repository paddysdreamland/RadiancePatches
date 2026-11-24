package dev.paddysdreamland.radiancepatches.memory;

import java.io.IOException;
import java.io.Writer;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.jemalloc.JEmalloc;

/**
 * Memory tracker (off by default, memory_tracker=true in radiance_patches.properties).
 * Every 30 seconds writes one line to logs/radiance_patches_memory.txt showing where the game's memory is:
 * the Java heap (what -Xmx limits), native memory the LWJGL allocator hands out (jemalloc, which -Xmx does NOT limit),
 * Minecraft/Radiance vertex buffers that are still open, and the vertex buffers Radiance has handed to its native
 * renderer and not given back yet. Comparing those against the whole process shows which part keeps growing.
 */
public final class MemoryTracker {
    private static final Path FILE = Path.of("logs", "radiance_patches_memory.txt");
    private static final long INTERVAL_MS = 30_000L;
    private static final double MB = 1024.0 * 1024.0;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Vertex buffers (ByteBufferBuilder), everything that is open right now.
    private static final LongAdder BUILDER_COUNT = new LongAdder();
    private static final LongAdder BUILDER_BYTES = new LongAdder();
    private static final LongAdder BUILDERS_EVER = new LongAdder();
    // Vertex buffers Radiance handed to its native renderer, not given back yet.
    private static final LongAdder CHUNK_HELD_COUNT = new LongAdder();
    private static final LongAdder CHUNK_HELD_BYTES = new LongAdder();
    private static final LongAdder ENTITY_HELD_COUNT = new LongAdder();
    private static final LongAdder ENTITY_HELD_BYTES = new LongAdder();

    private static volatile boolean started;
    private static boolean jemalloc;
    private static boolean jemallocStatsBroken;

    private MemoryTracker() {
    }

    // ---- counters, called from the mixins ----

    public static void builderOpened(long bytes) {
        BUILDER_COUNT.increment();
        BUILDERS_EVER.increment();
        BUILDER_BYTES.add(bytes);
    }

    public static void builderResized(long change) {
        BUILDER_BYTES.add(change);
    }

    public static void builderClosed(long bytes) {
        BUILDER_COUNT.decrement();
        BUILDER_BYTES.add(-bytes);
    }

    public static void handedToRadiance(boolean chunk, long bytes) {
        (chunk ? CHUNK_HELD_COUNT : ENTITY_HELD_COUNT).increment();
        (chunk ? CHUNK_HELD_BYTES : ENTITY_HELD_BYTES).add(bytes);
    }

    public static void givenBackByRadiance(boolean chunk, long bytes) {
        (chunk ? CHUNK_HELD_COUNT : ENTITY_HELD_COUNT).decrement();
        (chunk ? CHUNK_HELD_BYTES : ENTITY_HELD_BYTES).add(-bytes);
    }

    /** Writes an event line between the readings, when the tracker is running. */
    public static void note(String event) {
        if (started) {
            write(LocalDateTime.now().format(TIME) + "  # " + event + "\n", true);
        }
    }

    // ---- the report ----

    /** Starts the background thread that writes the report. Safe to call more than once. */
    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;
        Thread thread = new Thread(MemoryTracker::run, "Radiance Patches memory tracker");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private static void run() {
        String allocator = "unknown";
        try {
            allocator = MemoryUtil.getAllocator().getClass().getName();
            jemalloc = allocator.toLowerCase(java.util.Locale.ROOT).contains("jemalloc");
        } catch (Throwable error) {
            jemalloc = false;
        }
        write(header(allocator), false);
        while (true) {
            try {
                Thread.sleep(INTERVAL_MS);
                write(line(), true);
            } catch (InterruptedException error) {
                return;
            } catch (Throwable error) {
                write("# could not take a reading: " + error + "\n", true);
            }
        }
    }

    private static String header(String allocator) {
        return "# Radiance Patches memory tracker, started " + LocalDateTime.now().withNano(0) + "\n"
            + "# Native allocator (LWJGL): " + allocator + "\n"
            + "# Java max heap (-Xmx): " + mb(Runtime.getRuntime().maxMemory()) + "\n"
            + "#\n"
            + "# One line every 30 seconds. All sizes in MB.\n"
            + "#   process      = what Windows has committed for the whole game (Task Manager's \"Commit size\")\n"
            + "#   heap         = Java heap in use / reserved from Windows (this is the only part -Xmx limits)\n"
            + "#   direct       = Java direct buffers (the part -XX:MaxDirectMemorySize limits)\n"
            + "#   jemalloc     = native memory LWJGL's allocator has handed out / is actually holding from Windows\n"
            + "#                  (holding much more than handed out = freed memory the allocator kept instead of returning)\n"
            + "#   buffers      = vertex buffers open right now (count, size); made = how many were created in total\n"
            + "#   radiance     = vertex buffers handed to Radiance's native renderer and not given back yet\n"
            + "#   other native = process - heap reserved - non-heap - jemalloc holding (or open buffers without jemalloc):\n"
            + "#                  Radiance's core.dll, the graphics driver,\n"
            + "#                  Distant Horizons' own native memory, thread stacks and so on\n"
            + "#\n";
    }

    private static String line() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        MemoryUsage nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        long process = processCommitted();
        long direct = directBuffers();
        long[] je = jemallocStats();

        StringBuilder text = new StringBuilder();
        text.append(LocalDateTime.now().format(TIME)).append("  ").append(where());
        text.append(" | process ").append(mb(process));
        text.append(" | heap ").append(mb(heap.getUsed())).append(" / ").append(mb(heap.getCommitted()));
        text.append(" | non-heap ").append(mb(nonHeap.getCommitted()));
        text.append(" | direct ").append(mb(direct));
        if (je != null) {
            text.append(" | jemalloc ").append(mb(je[0])).append(" / ").append(mb(je[1]));
        }
        text.append(" | buffers ").append(BUILDER_COUNT.sum()).append(" = ").append(mb(BUILDER_BYTES.sum()))
            .append(" (made ").append(BUILDERS_EVER.sum()).append(')');
        text.append(" | radiance chunks ").append(CHUNK_HELD_COUNT.sum()).append(" = ").append(mb(CHUNK_HELD_BYTES.sum()));
        text.append(", entities ").append(ENTITY_HELD_COUNT.sum()).append(" = ").append(mb(ENTITY_HELD_BYTES.sum()));
        if (process > 0) {
            // With jemalloc the vertex buffers are part of what jemalloc holds; otherwise subtract them directly.
            long other = process - heap.getCommitted() - nonHeap.getCommitted() - (je != null ? je[1] : BUILDER_BYTES.sum());
            text.append(" | other native ").append(mb(other));
        }
        text.append('\n');
        return text.toString();
    }

    /** Dimension and entity count, or "menu" outside a world. */
    private static String where() {
        try {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return "menu";
            }
            return level.dimension().location() + " (" + level.getEntityCount() + " entities)";
        } catch (Throwable error) {
            return "?";
        }
    }

    /** The process's committed private memory (Windows "Commit size"), or -1 when the JVM can't tell. */
    private static long processCommitted() {
        try {
            if (ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean os) {
                return os.getCommittedVirtualMemorySize();
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private static long directBuffers() {
        for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
            if ("direct".equals(pool.getName())) {
                return pool.getMemoryUsed();
            }
        }
        return -1;
    }

    /** {allocated, resident} from jemalloc, or null when LWJGL isn't using jemalloc or its stats are unavailable. */
    private static long[] jemallocStats() {
        if (!jemalloc || jemallocStatsBroken) {
            return null;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // jemalloc only refreshes its statistics when the "epoch" is advanced.
            ByteBuffer epoch = stack.malloc(8);
            epoch.putLong(0, 1L);
            JEmalloc.je_mallctl("epoch", null, null, epoch);
            long allocated = jemallocValue(stack, "stats.allocated");
            long resident = jemallocValue(stack, "stats.resident");
            if (allocated < 0 || resident < 0) {
                jemallocStatsBroken = true;
                return null;
            }
            return new long[] {allocated, resident};
        } catch (Throwable error) {
            jemallocStatsBroken = true;
            return null;
        }
    }

    private static long jemallocValue(MemoryStack stack, String name) {
        ByteBuffer value = stack.malloc(8);
        PointerBuffer length = stack.mallocPointer(1);
        length.put(0, 8L);
        return JEmalloc.je_mallctl(name, value, length, null) == 0 ? value.getLong(0) : -1L;
    }

    private static String mb(long bytes) {
        return bytes < 0 ? "?" : String.format(java.util.Locale.ROOT, "%.0f", bytes / MB);
    }

    private static synchronized void write(String text, boolean append) {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                writer.write(text);
            }
        } catch (IOException error) {
            System.err.println("[Radiance Patches] Could not write " + FILE + ": " + error);
        }
    }
}
