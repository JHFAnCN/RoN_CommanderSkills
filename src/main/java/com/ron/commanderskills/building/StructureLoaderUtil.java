package com.ron.commanderskills.building;

import com.ron.commanderskills.RoNCommanderSkillsMod;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.LevelAccessor;

import java.io.InputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 从本 mod 命名空间加载建筑结构 NBT（主模组仅从 reignofnether 命名空间加载）。
 * 支持 ClassLoader 直读（不依赖 Level），供服务端放置/建造时使用。
 */
public final class StructureLoaderUtil {

    private static final String STRUCTURES_PREFIX = "data/" + RoNCommanderSkillsMod.MOD_ID + "/structures/";
    /** 缓存已加载的 NBT，避免服务端/多线程重复读 JAR */
    private static final ConcurrentHashMap<String, CompoundTag> NBT_CACHE = new ConcurrentHashMap<>();

    /**
     * 仅通过 ClassLoader 从本 mod JAR 加载 NBT（不依赖 Level/ResourceManager）。
     * 用于服务端创建放置时保证能拿到方块列表；结果会缓存。
     */
    public static CompoundTag getBuildingNbtFromModClassLoaderOnly(String structureName) {
        CompoundTag cached = NBT_CACHE.get(structureName);
        if (cached != null) return cached;

        String pathWithSlash = "/" + STRUCTURES_PREFIX + structureName + ".nbt";
        String pathNoSlash = STRUCTURES_PREFIX + structureName + ".nbt";
        String entryPath = STRUCTURES_PREFIX + structureName + ".nbt";

        CompoundTag nbt = readNbtFromClassLoader(pathWithSlash);
        if (nbt == null) nbt = readNbtFromClassLoader(pathNoSlash);
        if (nbt == null && Thread.currentThread().getContextClassLoader() != null) {
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(pathNoSlash)) {
                if (is != null) nbt = NbtIo.readCompressed(is);
            } catch (IOException e) {
                RoNCommanderSkillsMod.LOGGER.debug("ContextClassLoader load failed for {}: {}", pathNoSlash, e.getMessage());
            }
        }
        // 服务端常见：ClassLoader 拿不到 mod JAR 内 data/，用本类 CodeSource 直读（JAR 或开发目录）
        if (nbt == null) nbt = readNbtFromModByCodeSource(entryPath);
        if (nbt != null) NBT_CACHE.put(structureName, nbt);
        return nbt;
    }

    /**
     * 从本类所在 CodeSource 读 NBT：若为 jar 则用 JarURLConnection 打开条目流；若为目录（开发环境）则按 Gradle 布局找 resources 下文件。
     */
    private static CompoundTag readNbtFromModByCodeSource(String entryPath) {
        URL codeSource = StructureLoaderUtil.class.getProtectionDomain().getCodeSource().getLocation();
        if (codeSource == null) return null;

        // 1) jar:file:...!/ 形式：用 JarURLConnection 直接打开条目（不依赖当前线程 ClassLoader）
        try {
            String urlStr = codeSource.toString();
            if (urlStr.startsWith("jar:") && urlStr.contains("!")) {
                URL jarEntryUrl = new URL("jar:" + urlStr.replaceFirst("!.*", "!") + "/" + entryPath);
                try (InputStream is = jarEntryUrl.openStream()) {
                    CompoundTag nbt = NbtIo.readCompressed(is);
                    RoNCommanderSkillsMod.LOGGER.info("Structure loaded from mod JAR (JarURLConnection): {}", entryPath);
                    return nbt;
                }
            }
        } catch (Exception e) {
            RoNCommanderSkillsMod.LOGGER.debug("JarURLConnection load failed for {}: {}", entryPath, e.getMessage());
        }

        // 2) file: 路径：可能是 JAR 文件或开发环境下的 classes 目录
        try {
            String path = codeSource.getPath();
            if (path == null) return null;
            int sep = path.indexOf("!");
            if (sep >= 0) path = path.substring(0, sep);
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);
            if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') path = path.substring(1);
            Path basePath = Path.of(path);

            if (Files.isRegularFile(basePath)) {
                try (ZipFile zip = new ZipFile(basePath.toFile(), StandardCharsets.UTF_8)) {
                    ZipEntry entry = zip.getEntry(entryPath);
                    if (entry != null) {
                        try (InputStream is = zip.getInputStream(entry)) {
                            CompoundTag nbt = NbtIo.readCompressed(is);
                            RoNCommanderSkillsMod.LOGGER.info("Structure loaded from mod JAR (ZipFile): {}", entryPath);
                            return nbt;
                        }
                    }
                }
            } else if (Files.isDirectory(basePath)) {
                // 开发环境：basePath 常为 .../build/classes/java/main，资源在 .../build/resources/main/
                Path resourcesMain = basePath.resolve("..").resolve("..").resolve("..").resolve("resources").resolve("main").normalize();
                Path filePath = resourcesMain.resolve(entryPath);
                if (Files.isRegularFile(filePath)) {
                    try (InputStream is = Files.newInputStream(filePath)) {
                        CompoundTag nbt = NbtIo.readCompressed(is);
                        RoNCommanderSkillsMod.LOGGER.info("Structure loaded from dev path: {}", filePath);
                        return nbt;
                    }
                }
            }
        } catch (Exception e) {
            RoNCommanderSkillsMod.LOGGER.debug("readNbtFromModByCodeSource path failed for {}: {}", entryPath, e.getMessage());
        }
        return null;
    }

    /**
     * 仅通过 ClassLoader 加载并转为 BuildingBlock 列表（不依赖 Level）。用于 createBuildingPlacement 时 blocks 为空的重试。
     */
    public static ArrayList<BuildingBlock> getBuildingBlocksFromModStructureClassLoaderOnly(String structureName) {
        CompoundTag nbt = getBuildingNbtFromModClassLoaderOnly(structureName);
        if (nbt == null) return new ArrayList<>();
        return BuildingBlockData.getBuildingBlocksFromNbt(nbt);
    }

    /**
     * 从 mod 资源加载结构 NBT。路径为 data/&lt;modid&gt;/structures/&lt;structureName&gt;.nbt
     * 服务端优先用 ClassLoader（并缓存），客户端先试 ResourceManager 再 ClassLoader。
     */
    public static CompoundTag getBuildingNbtFromMod(String structureName, LevelAccessor level) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(RoNCommanderSkillsMod.MOD_ID, "structures/" + structureName + ".nbt");
        String classPathPath = "/" + STRUCTURES_PREFIX + structureName + ".nbt";

        // 服务端：优先 ClassLoader（放置/建造在服务端执行）
        if (!level.isClientSide()) {
            CompoundTag fromJar = getBuildingNbtFromModClassLoaderOnly(structureName);
            if (fromJar != null) {
                RoNCommanderSkillsMod.LOGGER.debug("Structure loaded from mod JAR (server): {}", classPathPath);
                return fromJar;
            }
        }

        // 1) 尝试 ResourceManager（客户端预览等）
        try {
            ResourceManager resourceManager = level.isClientSide()
                    ? Minecraft.getInstance().getResourceManager()
                    : level.getServer().getResourceManager();
            Optional<Resource> rs = resourceManager.getResource(rl);
            if (rs.isPresent()) {
                CompoundTag nbt = NbtIo.readCompressed(rs.get().open());
                if (nbt != null) {
                    RoNCommanderSkillsMod.LOGGER.debug("Structure loaded via ResourceManager: {}", rl);
                    NBT_CACHE.put(structureName, nbt);
                    return nbt;
                }
            }
        } catch (Exception e) {
            RoNCommanderSkillsMod.LOGGER.debug("ResourceManager failed for {}: {}", rl, e.getMessage());
        }

        // 2) 回退：ClassLoader（含多加载器尝试与缓存）
        CompoundTag fromJar = getBuildingNbtFromModClassLoaderOnly(structureName);
        if (fromJar != null) {
            RoNCommanderSkillsMod.LOGGER.info("Structure loaded from mod JAR: {}", classPathPath);
            return fromJar;
        }

        RoNCommanderSkillsMod.LOGGER.warn("Structure not found: {} (tried ResourceManager and ClassLoader)", rl);
        return null;
    }

    private static CompoundTag readNbtFromClassLoader(String classPathPath) {
        try (InputStream is = StructureLoaderUtil.class.getResourceAsStream(classPathPath)) {
            if (is != null) return NbtIo.readCompressed(is);
        } catch (IOException e) {
            RoNCommanderSkillsMod.LOGGER.debug("ClassLoader load failed for {}: {}", classPathPath, e.getMessage());
        }
        return null;
    }

    /**
     * 从本 mod 加载结构并转换为 BuildingBlock 列表。若文件不存在或解析失败则返回空列表。
     */
    public static ArrayList<BuildingBlock> getBuildingBlocksFromModStructure(String structureName, LevelAccessor level) {
        CompoundTag nbt = getBuildingNbtFromMod(structureName, level);
        if (nbt == null) return new ArrayList<>();
        return BuildingBlockData.getBuildingBlocksFromNbt(nbt);
    }
}
