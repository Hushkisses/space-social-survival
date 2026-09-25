package com.hushkisses.spacesurvival.paper.command;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.resource.ResourceStore;
import com.hushkisses.spacesurvival.resource.ResourceType;
import com.hushkisses.spacesurvival.resource.node.*;
import com.hushkisses.spacesurvival.resource.processing.ProcessingRecipe;
import com.hushkisses.spacesurvival.resource.processing.ProcessingRecipeId;
import com.hushkisses.spacesurvival.resource.processing.ProcessingResult;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class ResourceCommandHandler {

    private final SpaceSurvivalPlugin plugin;

    public ResourceCommandHandler(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "status" -> handleStatus(sender, args);
            case "add" -> handleAdd(sender, args);
            case "process" -> handleProcess(sender, args);
            case "item" -> handleItem(sender, args);
            case "nodes" -> handleNodes(sender, args);
            case "itemsadder" -> {
                sender.sendMessage(
                        "§7ItemsAdder: "
                                + (plugin.resourceItemProvider().customItemsAvailable()
                                ? "§a연결됨"
                                : "§e미설치/미연결 — 바닐라 아이템 폴백 사용")
                );
                yield true;
            }
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleStatus(CommandSender sender, String[] args) {
        String scope = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "shared";
        ResourceStore store = resolveStore(sender, scope);
        if (store == null) return true;

        sender.sendMessage("§6[우주 생존] §f자원 현황 (" + scopeName(scope) + ")");
        Map<ResourceType, Integer> snapshot = store.snapshot();
        for (ResourceType type : ResourceType.values()) {
            sender.sendMessage("§7" + type.name() + ": §f" + snapshot.getOrDefault(type, 0));
        }
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c자원 추가 권한이 없습니다.");
            return true;
        }
        if (args.length < 5) {
            sender.sendMessage("§c사용법: /space resource add <shared|self> <type> <amount>");
            return true;
        }

        ResourceStore store = resolveStore(sender, args[2]);
        if (store == null) return true;

        ResourceType type = parseType(sender, args[3]);
        if (type == null) return true;

        int amount = parsePositive(sender, args[4]);
        if (amount < 0) return true;

        store.add(type, amount);
        sender.sendMessage("§a자원을 추가했습니다: " + type.name() + " x" + amount);
        return true;
    }

    private boolean handleProcess(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /space resource process <shared|self> <recipeId>");
            sender.sendMessage("§7레시피: circuit_salvage, power_cell_refurbish");
            return true;
        }

        ResourceStore store = resolveStore(sender, args[2]);
        if (store == null) return true;

        ProcessingRecipe recipe = plugin.processingRegistry()
                .find(new ProcessingRecipeId(args[3].toLowerCase(Locale.ROOT)))
                .orElse(null);
        if (recipe == null) {
            sender.sendMessage("§c존재하지 않는 가공 레시피입니다.");
            return true;
        }

        ProcessingResult result = plugin.processingService().process(store, recipe);
        if (result == ProcessingResult.SUCCESS) {
            sender.sendMessage("§a가공 완료: " + recipe.displayName());
        } else {
            sender.sendMessage("§c가공에 필요한 자원이 부족합니다.");
        }
        return true;
    }

    private boolean handleItem(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c아이템 생성은 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c아이템 생성 권한이 없습니다.");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /space resource item <type> <amount>");
            return true;
        }

        ResourceType type = parseType(sender, args[2]);
        if (type == null) return true;
        int amount = parsePositive(sender, args[3]);
        if (amount < 0) return true;

        ItemStack item = plugin.resourceItemProvider().create(type, amount);
        player.getInventory().addItem(item);
        sender.sendMessage("§a표현용 자원 아이템을 지급했습니다: " + type.name());
        return true;
    }

    private boolean handleNodes(CommandSender sender, String[] args) {
        long seed;
        if (args.length >= 3) {
            try {
                seed = Long.parseLong(args[2]);
            } catch (NumberFormatException exception) {
                sender.sendMessage("§c시드는 정수여야 합니다.");
                return true;
            }
        } else {
            seed = ThreadLocalRandom.current().nextLong();
        }

        List<ResourceNode> nodes = new ResourceNodeGenerator().generate(
                debugSlots(),
                new ResourceNodeGenerationConfig(7, 10, 1, 3),
                new Random(seed)
        );

        sender.sendMessage("§6[우주 생존] §f자원 노드 생성 시드: " + seed);
        for (ResourceNode node : nodes) {
            sender.sendMessage(
                    "§7- §f" + node.resourceType().name()
                            + " x" + node.quantity()
                            + " §8@" + node.spawnSlotId()
                            + "/" + node.facilityId()
            );
        }
        return true;
    }

    private ResourceStore resolveStore(CommandSender sender, String scope) {
        if (scope.equalsIgnoreCase("shared")) {
            return plugin.resourceLedger().shared();
        }
        if (scope.equalsIgnoreCase("self")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cself 저장소는 게임 안의 플레이어만 사용할 수 있습니다.");
                return null;
            }
            return plugin.resourceLedger().personal(PlayerId.of(player.getUniqueId()));
        }

        sender.sendMessage("§c저장소는 shared 또는 self여야 합니다.");
        return null;
    }

    private static ResourceType parseType(CommandSender sender, String value) {
        try {
            return ResourceType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("§c알 수 없는 자원입니다. repair_parts, circuits, power_cells, fuel, medical_supplies, bio_samples, data_cores");
            return null;
        }
    }

    private static int parsePositive(CommandSender sender, String value) {
        try {
            int amount = Integer.parseInt(value);
            if (amount < 1) throw new NumberFormatException();
            return amount;
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c수량은 1 이상의 정수여야 합니다.");
            return -1;
        }
    }

    private static String scopeName(String scope) {
        return scope.equalsIgnoreCase("self") ? "개인" : "공용";
    }

    private static List<ResourceSpawnSlot> debugSlots() {
        return List.of(
                new ResourceSpawnSlot("bridge-a", DefaultFacilityCatalog.BRIDGE),
                new ResourceSpawnSlot("bridge-b", DefaultFacilityCatalog.BRIDGE),
                new ResourceSpawnSlot("engineering-a", DefaultFacilityCatalog.ENGINEERING),
                new ResourceSpawnSlot("engineering-b", DefaultFacilityCatalog.ENGINEERING),
                new ResourceSpawnSlot("engineering-c", DefaultFacilityCatalog.ENGINEERING),
                new ResourceSpawnSlot("medical-a", DefaultFacilityCatalog.MEDICAL),
                new ResourceSpawnSlot("medical-b", DefaultFacilityCatalog.MEDICAL),
                new ResourceSpawnSlot("research-a", DefaultFacilityCatalog.RESEARCH),
                new ResourceSpawnSlot("research-b", DefaultFacilityCatalog.RESEARCH),
                new ResourceSpawnSlot("cargo-a", DefaultFacilityCatalog.CARGO),
                new ResourceSpawnSlot("cargo-b", DefaultFacilityCatalog.CARGO),
                new ResourceSpawnSlot("habitation-a", DefaultFacilityCatalog.HABITATION)
        );
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space resource status [shared|self]");
        sender.sendMessage("§c사용법: /space resource add <shared|self> <type> <amount>");
        sender.sendMessage("§c사용법: /space resource process <shared|self> <recipeId>");
        sender.sendMessage("§c사용법: /space resource item <type> <amount>");
        sender.sendMessage("§c사용법: /space resource nodes [seed]");
        sender.sendMessage("§c사용법: /space resource itemsadder");
    }
}
