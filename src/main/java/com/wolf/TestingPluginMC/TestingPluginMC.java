package com.wolf.testingpluginmc;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.Material;
import org.bukkit.block.Block;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.Random;

public class TestingPluginMC extends JavaPlugin implements Listener {

    private File dataFile;
    private FileConfiguration playerData;
    private HashMap<UUID, Boolean> minerJobs;
    private HashMap<Material, Double> blockValues;
    private HashMap<Material, Integer> blockXP;
    private Random random;

    @Override
    public void onEnable() {
        getLogger().info("§a══════════════════════════════");
        getLogger().info("§aТестинг-Плагин МС активирован!");
        getLogger().info("§aВерсия: 1.0 - Русская экономика");
        getLogger().info("§a══════════════════════════════");

        // Инициализация
        minerJobs = new HashMap<>();
        blockValues = new HashMap<>();
        blockXP = new HashMap<>();
        random = new Random();

        // Настройка цен за блоки
        setupBlockValues();

        // Регистрация событий
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);

        // Создание конфигурации
        saveDefaultConfig();
        setupDataFile();

        // Регистрация команд
        getCommand("eco").setExecutor(this);
        getCommand("miner").setExecutor(this);
        getCommand("rubli").setExecutor(this);
        getCommand("topminers").setExecutor(this);

        // Авто-сохранение каждые 5 минут
        getServer().getScheduler().runTaskTimer(this, this::saveData, 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        saveData();
        getLogger().info("§cТестинг-Плагин МС деактивирован!");
    }

    private void setupBlockValues() {
        // Значения в рублях за блок
        blockValues.put(Material.COAL_ORE, 5.0);
        blockValues.put(Material.DEEPSLATE_COAL_ORE, 6.0);
        blockValues.put(Material.IRON_ORE, 15.0);
        blockValues.put(Material.DEEPSLATE_IRON_ORE, 18.0);
        blockValues.put(Material.COPPER_ORE, 8.0);
        blockValues.put(Material.DEEPSLATE_COPPER_ORE, 9.5);
        blockValues.put(Material.GOLD_ORE, 25.0);
        blockValues.put(Material.DEEPSLATE_GOLD_ORE, 30.0);
        blockValues.put(Material.REDSTONE_ORE, 12.0);
        blockValues.put(Material.DEEPSLATE_REDSTONE_ORE, 14.0);
        blockValues.put(Material.LAPIS_ORE, 20.0);
        blockValues.put(Material.DEEPSLATE_LAPIS_ORE, 24.0);
        blockValues.put(Material.DIAMOND_ORE, 100.0);
        blockValues.put(Material.DEEPSLATE_DIAMOND_ORE, 120.0);
        blockValues.put(Material.EMERALD_ORE, 150.0);
        blockValues.put(Material.DEEPSLATE_EMERALD_ORE, 180.0);
        blockValues.put(Material.NETHER_QUARTZ_ORE, 7.0);
        blockValues.put(Material.NETHER_GOLD_ORE, 22.0);
        blockValues.put(Material.ANCIENT_DEBRIS, 500.0);

        // Опыт за блоки
        blockXP.put(Material.COAL_ORE, 5);
        blockXP.put(Material.IRON_ORE, 10);
        blockXP.put(Material.COPPER_ORE, 7);
        blockXP.put(Material.GOLD_ORE, 15);
        blockXP.put(Material.REDSTONE_ORE, 8);
        blockXP.put(Material.LAPIS_ORE, 12);
        blockXP.put(Material.DIAMOND_ORE, 50);
        blockXP.put(Material.EMERALD_ORE, 75);
        blockXP.put(Material.ANCIENT_DEBRIS, 100);
    }

    private void setupDataFile() {
        dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerData = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveData() {
        try {
            playerData.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double getBalance(UUID uuid) {
        return playerData.getDouble(uuid.toString() + ".balance", 0.0);
    }

    private void setBalance(UUID uuid, double amount) {
        playerData.set(uuid.toString() + ".balance", amount);
    }

    private void addBalance(UUID uuid, double amount) {
        double current = getBalance(uuid);
        setBalance(uuid, current + amount);
    }

    private int getMinedBlocks(UUID uuid) {
        return playerData.getInt(uuid.toString() + ".mined_blocks", 0);
    }

    private void addMinedBlock(UUID uuid) {
        int current = getMinedBlocks(uuid);
        playerData.set(uuid.toString() + ".mined_blocks", current + 1);
    }

    private double getTotalEarned(UUID uuid) {
        return playerData.getDouble(uuid.toString() + ".total_earned", 0.0);
    }

    private void addTotalEarned(UUID uuid, double amount) {
        double current = getTotalEarned(uuid);
        playerData.set(uuid.toString() + ".total_earned", current + amount);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Проверяем, работает ли игрок шахтёром
        if (!minerJobs.getOrDefault(uuid, false)) {
            return;
        }

        Block block = event.getBlock();
        Material type = block.getType();

        // Проверяем, есть ли у блока стоимость
        if (blockValues.containsKey(type)) {
            double value = blockValues.get(type);
            int xp = blockXP.getOrDefault(type, 5);

            // Добавляем случайный бонус (1-20%)
            double bonusMultiplier = 1.0 + (random.nextDouble() * 0.2);
            double finalValue = value * bonusMultiplier;

            // Добавляем деньги и опыт
            addBalance(uuid, finalValue);
            addTotalEarned(uuid, finalValue);
            addMinedBlock(uuid);

            player.giveExp(xp);

            // Эффекты и сообщения
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3);

            // Красивое сообщение
            String message = String.format(
                    "§e⚒️ §fВы добыли §6%s§f! §a+%.1f₽ §7(бонус: +%.0f%%) §e+%d опыта",
                    getBlockName(type),
                    finalValue,
                    (bonusMultiplier - 1) * 100,
                    xp
            );
            player.sendMessage(message);

            // Шанс на редкое сообщение
            if (random.nextInt(100) < 5) {
                String[] rareMessages = {
                        "§6✨ Отличная работа, товарищ шахтёр!",
                        "§6💎 Вы настоящий мастер горного дела!",
                        "§6🏆 Русские шахтёры - самые лучшие!",
                        "§6⭐ За такую работу положена премия!"
                };
                player.sendMessage(rareMessages[random.nextInt(rareMessages.length)]);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            }
        }
    }

    private String getBlockName(Material material) {
        HashMap<Material, String> names = new HashMap<>();
        names.put(Material.COAL_ORE, "Угольная руда");
        names.put(Material.DEEPSLATE_COAL_ORE, "Глубинносланцевая угольная руда");
        names.put(Material.IRON_ORE, "Железная руда");
        names.put(Material.DEEPSLATE_IRON_ORE, "Глубинносланцевая железная руда");
        names.put(Material.COPPER_ORE, "Медная руда");
        names.put(Material.DEEPSLATE_COPPER_ORE, "Глубинносланцевая медная руда");
        names.put(Material.GOLD_ORE, "Золотая руда");
        names.put(Material.DEEPSLATE_GOLD_ORE, "Глубинносланцевая золотая руда");
        names.put(Material.REDSTONE_ORE, "Редстоуновая руда");
        names.put(Material.DEEPSLATE_REDSTONE_ORE, "Глубинносланцевая редстоуновая руда");
        names.put(Material.LAPIS_ORE, "Лазуритовая руда");
        names.put(Material.DEEPSLATE_LAPIS_ORE, "Глубинносланцевая лазуритовая руда");
        names.put(Material.DIAMOND_ORE, "Алмазная руда");
        names.put(Material.DEEPSLATE_DIAMOND_ORE, "Глубинносланцевая алмазная руда");
        names.put(Material.EMERALD_ORE, "Изумрудная руда");
        names.put(Material.DEEPSLATE_EMERALD_ORE, "Глубинносланцевая изумрудная руда");
        names.put(Material.NETHER_QUARTZ_ORE, "Незер-кварцевая руда");
        names.put(Material.NETHER_GOLD_ORE, "Незер-золотая руда");
        names.put(Material.ANCIENT_DEBRIS, "Древние обломки");

        return names.getOrDefault(material, material.toString());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cЭта команда только для игроков!");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (cmd.getName().equalsIgnoreCase("eco")) {
            return handleEcoCommand(player, args);
        } else if (cmd.getName().equalsIgnoreCase("miner")) {
            return handleMinerCommand(player, args);
        } else if (cmd.getName().equalsIgnoreCase("rubli")) {
            return handleRubliCommand(player, args);
        } else if (cmd.getName().equalsIgnoreCase("topminers")) {
            return handleTopMinersCommand(player);
        }

        return false;
    }

    private boolean handleEcoCommand(Player player, String[] args) {
        UUID uuid = player.getUniqueId();
        double balance = getBalance(uuid);
        int minedBlocks = getMinedBlocks(uuid);
        double totalEarned = getTotalEarned(uuid);

        // Красивый интерфейс баланса
        player.sendMessage("§8══════════════════════════════");
        player.sendMessage("§6🏦 §fБанк Российской Федерации");
        player.sendMessage("§8──────────────────────────────");
        player.sendMessage(String.format("§7👤 Игрок: §f%s", player.getName()));
        player.sendMessage(String.format("§7💰 Баланс: §a%.2f₽", balance));
        player.sendMessage(String.format("§7⚒️ Добыто блоков: §e%,d", minedBlocks));
        player.sendMessage(String.format("§7💎 Всего заработано: §a%,.2f₽", totalEarned));

        // Проверка ранга
        String rank;
        if (totalEarned >= 100000) rank = "§6👑 Владелец шахты";
        else if (totalEarned >= 50000) rank = "§d💎 Директор рудника";
        else if (totalEarned >= 10000) rank = "§c⭐ Старший шахтёр";
        else if (totalEarned >= 1000) rank = "§b🏅 Шахтёр";
        else if (totalEarned > 0) rank = "§a🔨 Новичок";
        else rank = "§7❓ Безработный";

        player.sendMessage(String.format("§7🏆 Ранг: %s", rank));
        player.sendMessage("§8══════════════════════════════");

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
        return true;
    }

    private boolean handleMinerCommand(Player player, String[] args) {
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            boolean isMiner = minerJobs.getOrDefault(uuid, false);

            if (isMiner) {
                player.sendMessage("§8══════════════════════════════");
                player.sendMessage("§6⚒️ Статус шахтёра");
                player.sendMessage("§8──────────────────────────────");
                player.sendMessage("§a✅ Вы уже работаете шахтёром!");
                player.sendMessage("§7Добывайте руду для заработка.");
                player.sendMessage("§7Используйте: §f/miner quit §7- уволиться");
                player.sendMessage("§7Используйте: §f/miner stats §7- статистика");
                player.sendMessage("§8══════════════════════════════");
            } else {
                player.sendMessage("§8══════════════════════════════");
                player.sendMessage("§6⚒️ Шахтёрская работа");
                player.sendMessage("§8──────────────────────────────");
                player.sendMessage("§fДобро пожаловать в шахтёрскую гильдию!");
                player.sendMessage("§7Зарабатывайте деньги, добывая руду.");
                player.sendMessage("§7Для начала работы напишите:");
                player.sendMessage("§a/miner join");
                player.sendMessage("§8══════════════════════════════");
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                if (minerJobs.containsKey(uuid) && minerJobs.get(uuid)) {
                    player.sendMessage("§cВы уже работаете шахтёром!");
                    return true;
                }

                minerJobs.put(uuid, true);
                player.sendMessage("§8══════════════════════════════");
                player.sendMessage("§a✅ Вы устроились шахтёром!");
                player.sendMessage("§7Теперь при добыче руды вы будете");
                player.sendMessage("§7получать деньги и опыт!");
                player.sendMessage("§6Удачи в работе, товарищ!");
                player.sendMessage("§8══════════════════════════════");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                break;

            case "quit":
                if (!minerJobs.getOrDefault(uuid, false)) {
                    player.sendMessage("§cВы не работаете шахтёром!");
                    return true;
                }

                minerJobs.put(uuid, false);
                player.sendMessage("§8══════════════════════════════");
                player.sendMessage("§c🚫 Вы уволились с работы шахтёра");
                player.sendMessage("§7Вы больше не будете получать");
                player.sendMessage("§7деньги за добытую руду.");
                player.sendMessage("§8══════════════════════════════");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                break;

            case "stats":
                int mined = getMinedBlocks(uuid);
                double earned = getTotalEarned(uuid);

                player.sendMessage("§8══════════════════════════════");
                player.sendMessage("§6📊 Статистика шахтёра");
                player.sendMessage("§8──────────────────────────────");
                player.sendMessage(String.format("§7Добыто блоков: §e%,d", mined));
                player.sendMessage(String.format("§7Заработано всего: §a%,.2f₽", earned));

                // Средний заработок за блок
                if (mined > 0) {
                    double avg = earned / mined;
                    player.sendMessage(String.format("§7Средний доход за блок: §a%.2f₽", avg));
                }

                player.sendMessage("§8══════════════════════════════");
                break;

            case "prices":
                player.sendMessage("§8══════════════════════════════");
                player.sendMessage("§6💰 Цены за руду");
                player.sendMessage("§8──────────────────────────────");
                blockValues.forEach((material, value) -> {
                    player.sendMessage(String.format("§7%s: §a%.1f₽", getBlockName(material), value));
                });
                player.sendMessage("§8══════════════════════════════");
                break;

            default:
                player.sendMessage("§cИспользование: /miner [join|quit|stats|prices]");
                break;
        }

        return true;
    }

    private boolean handleRubliCommand(Player player, String[] args) {
        if (args.length == 3 && args[0].equalsIgnoreCase("give") && player.hasPermission("testingpluginmc.admin")) {
            Player target = getServer().getPlayer(args[1]);
            if (target == null) {
                player.sendMessage("§cИгрок не найден!");
                return true;
            }

            try {
                double amount = Double.parseDouble(args[2]);
                if (amount <= 0) {
                    player.sendMessage("§cСумма должна быть положительной!");
                    return true;
                }

                addBalance(target.getUniqueId(), amount);
                player.sendMessage(String.format("§aВы выдали %.2f₽ игроку %s", amount, target.getName()));
                target.sendMessage(String.format("§aАдминистратор %s выдал вам %.2f₽", player.getName(), amount));
                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            } catch (NumberFormatException e) {
                player.sendMessage("§cНеверная сумма!");
            }
            return true;
        }

        player.sendMessage("§cИспользование: /rubli give <игрок> <сумма>");
        return true;
    }

    private boolean handleTopMinersCommand(Player player) {
        player.sendMessage("§8══════════════════════════════");
        player.sendMessage("§6🏆 Топ шахтёров сервера");
        player.sendMessage("§8──────────────────────────────");
        player.sendMessage("§7(Эта функция в разработке)");
        player.sendMessage("§7Скоро здесь появится рейтинг!");
        player.sendMessage("§8══════════════════════════════");
        return true;
    }
}