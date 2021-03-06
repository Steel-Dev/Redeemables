package com.github.steeldev.redeemables.systems;

import com.github.steeldev.redeemables.Redeemables;
import jdk.internal.jline.internal.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RedeemSystem {
    static Redeemables main = Redeemables.getInstance();

    static String[] alpnum = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

    public static String generateRedeemCode() throws IOException {
        StringBuilder result = new StringBuilder();

        String codeType = (main.config.getString("Code.Type") != null) ? main.config.getString("Code.Type") : "short";

        if (!codeType.equalsIgnoreCase("short") &&
                !codeType.equalsIgnoreCase("medium") &&
                !codeType.equalsIgnoreCase("long"))
            codeType = "short";

        int codeLength = 0;

        switch (codeType.toLowerCase()) {
            case "short":
                codeLength = 4;
                break;
            case "medium":
                codeLength = 8;
                break;
            case "long":
                codeLength = 12;
                break;
        }
        int dashOffset = 4;
        String codePrefix = main.config.getString("Code.Prefix");

        int temp = 0;
        for (int i = 0; i < codeLength; i++) {
            String resAdd = alpnum[main.rand.nextInt(alpnum.length)];
            if (main.chanceOf(80)) resAdd = resAdd.toUpperCase();
            result.append(resAdd);
            temp++;
            if (temp == dashOffset && i != codeLength - 1) {
                result.append("-");
                temp = 0;
            }
        }
        return result.insert(0, codePrefix).toString();
    }

    public static void createRedeem(String type, @Nullable ItemStack item, int amount, int maxRedeems, String code, String display) throws IOException {
        if (main.redeemCodes.get("Codes." + code) != null) return;

        main.redeemCodes.set("Codes." + code + ".RedeemDisplay", display.replaceAll("_", " "));
        main.redeemCodes.set("Codes." + code + ".Type", type);
        main.redeemCodes.set("Codes." + code + ".Amount", amount);
        main.redeemCodes.set("Codes." + code + ".MaxRedeems", maxRedeems);
        main.redeemCodes.set("Codes." + code + ".RedeemedBy", new ArrayList<String>());

        String action = main.config.getString("RedeemTypes." + type + ".Action");

        if (action.equalsIgnoreCase("GiveItem")) {
            if (item == null) return;
            main.redeemCodes.set("Codes." + code + ".Item", item);
        }
        main.redeemCodes.save(main.redeemCodesFile);
    }

    public static RedeemState attemptRedeemCode(String code, Player player) {
        if (main.redeemCodes.get("Codes." + code) == null) return RedeemState.CODE_INVALID;

        String type = main.redeemCodes.getString("Codes." + code + ".Type");
        int amount = main.redeemCodes.getInt("Codes." + code + ".Amount");
        int maxRedeems = main.redeemCodes.getInt("Codes." + code + ".MaxRedeems");
        List<String> redeemedBy = main.redeemCodes.getStringList("Codes." + code + ".RedeemedBy");
        ItemStack item = main.redeemCodes.getItemStack("Codes." + code + ".Item");

        if (redeemedBy.size() >= maxRedeems && maxRedeems > 0) return RedeemState.CANT_BE_REDEEMED;

        if (redeemedBy.contains(player.getUniqueId().toString())) return RedeemState.ALREADY_REDEEMED;

        if (amount == 0) return RedeemState.ERROR;
        String action = main.config.getString("RedeemTypes." + type + ".Action");
        if (action.equalsIgnoreCase("GiveItem")) {
            if (item == null) return RedeemState.ERROR;
            if (player.getInventory().firstEmpty() == -1) return RedeemState.INVENTORY_FULL;
        }

        return RedeemState.SUCCESS;
    }

    public enum RedeemState {
        SUCCESS,
        CODE_INVALID,
        CANT_BE_REDEEMED,
        ALREADY_REDEEMED,
        INVENTORY_FULL,
        ERROR
    }
}
