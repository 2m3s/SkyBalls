package com.epic60869.skyballs;

import com.epic60869.skyballs.features.sbc.Sbc;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * /sb casino: every casino game in one screen with tabs (blackjack opens its own table). The server decides every
 * result; this screen only sends bets and animates what came back: the coin spins, the dice number rolls, the
 * roulette wheel slows down, the slot reels stop one by one, the higher/lower card flips.
 */
public final class SkyBallsCasinoGamesScreen extends Screen {
    enum Tab {
        BLACKJACK("Blackjack", "blackjack"), COINFLIP("Coinflip", "coinflip"), DICE("Dice", "dice"),
        ROULETTE("Roulette", "roulette"), SLOTS("Slots", "slots"), HILO("Hi-Lo", "hilo"),
        DAILY("Daily", "daily"), STATS("Stats", "stats"), LEADERBOARD("Top", "leaderboard");

        final String label;
        final String game;

        Tab(String label, String game) {
            this.label = label;
            this.game = game;
        }
    }

    private static final int W = 360;
    private static final int H = 240;
    private static final int FELT = 0xFF0F4D2E;
    private static final int FELT_DARK = 0xFF0A3620;
    private static final int GOLD = 0xFFFFD35A;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF9AA5B8;
    private static final long ANIMATION_MS = 1600;
    private static final String[] SYMBOLS = {"CHERRY", "LEMON", "ORANGE", "BELL", "BAR", "SEVEN", "DIAMOND"};
    private static final Map<String, Item> SYMBOL_ITEMS = Map.of("CHERRY", Items.SWEET_BERRIES, "LEMON", Items.HONEYCOMB,
        "ORANGE", Items.PUMPKIN_PIE, "BELL", Items.BELL, "BAR", Items.GOLD_INGOT, "SEVEN", Items.NETHER_STAR, "DIAMOND", Items.DIAMOND);
    private static final String[] BOARDS = {"balance", "biggestWin", "wagered", "streak"};
    private static final String[] ROULETTE_BETS = {"red", "black", "odd", "even", "low", "high", "dozen1", "dozen2", "dozen3", "column1", "column2", "column3"};
    private static final int[] RED_NUMBERS = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36};

    static Tab tab = Tab.COINFLIP;
    private static long lastBet = 10;
    private static int diceChance = 50;
    private static String rouletteBet = "red";
    private static String board = "balance";

    private final Random random = new Random();
    private final List<Button> gameButtons = new ArrayList<>();
    private EditBox betBox;
    private EditBox chanceBox;
    private EditBox numberBox;
    private long statsAskedAt;
    private long boardAskedAt;

    public SkyBallsCasinoGamesScreen() {
        super(Component.literal("SkyBalls Casino"));
    }

    public SkyBallsCasinoGamesScreen(Tab tab) {
        this();
        SkyBallsCasinoGamesScreen.tab = tab;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2 + 10;
    }

    @Override
    protected void init() {
        gameButtons.clear();
        int x = (width - Tab.values().length * 40) / 2;
        for (Tab t : Tab.values()) {
            Button b = addRenderableWidget(Button.builder(Component.literal(t.label), btn -> switchTo(t))
                .bounds(x, top() - 24, 39, 18).build());
            b.active = t != tab;
            x += 40;
        }
        int left = left();
        int bottom = top() + H;
        boolean betting = tab == Tab.COINFLIP || tab == Tab.DICE || tab == Tab.ROULETTE || tab == Tab.SLOTS || tab == Tab.HILO;
        if (betting) {
            String bet = betBox == null ? String.valueOf(lastBet) : betBox.getValue();
            betBox = addRenderableWidget(new EditBox(font, left + 8, bottom - 50, 60, 18, Component.literal("Bet")));
            betBox.setMaxLength(12);
            betBox.setValue(bet);
            long[] amounts = {10, 25, 100, -1};
            for (int i = 0; i < amounts.length; i++) {
                long amount = amounts[i];
                addRenderableWidget(Button.builder(Component.literal(amount < 0 ? "All in" : "$" + amount),
                        b -> betBox.setValue(String.valueOf(amount < 0 ? Math.max(0, SkyBallsCasino.balance) : amount)))
                    .bounds(left + 72 + i * 44, bottom - 51, amount < 0 ? 46 : 42, 20).build());
            }
        }
        int by = bottom - 27;
        switch (tab) {
            case COINFLIP -> {
                game(Button.builder(Component.literal("Heads"), b -> play(a -> SkyBallsCasino.coinflip(a, "heads"))).bounds(left + 8, by, 110, 20).build());
                game(Button.builder(Component.literal("Tails"), b -> play(a -> SkyBallsCasino.coinflip(a, "tails"))).bounds(left + 124, by, 110, 20).build());
            }
            case DICE -> {
                chanceBox = addRenderableWidget(new EditBox(font, left + 264, bottom - 50, 40, 18, Component.literal("Chance")));
                chanceBox.setMaxLength(2);
                chanceBox.setValue(String.valueOf(diceChance));
                chanceBox.setResponder(v -> {
                    try {
                        diceChance = Math.max(1, Math.min(95, Integer.parseInt(v.trim())));
                    } catch (NumberFormatException ignored) {}
                });
                game(Button.builder(Component.literal("Roll under"), b -> play(a -> SkyBallsCasino.dice(a, diceChance, "under"))).bounds(left + 8, by, 110, 20).build());
                game(Button.builder(Component.literal("Roll over"), b -> play(a -> SkyBallsCasino.dice(a, diceChance, "over"))).bounds(left + 124, by, 110, 20).build());
            }
            case ROULETTE -> {
                for (int i = 0; i < ROULETTE_BETS.length; i++) {
                    String bet = ROULETTE_BETS[i];
                    Button b = addRenderableWidget(Button.builder(Component.literal(rouletteLabel(bet)), btn -> {
                            rouletteBet = bet;
                            rebuildWidgets();
                        })
                        .bounds(left + 8 + (i % 6) * 57, top() + 124 + (i / 6) * 20, 56, 18).build());
                    b.active = !bet.equals(rouletteBet);
                }
                numberBox = addRenderableWidget(new EditBox(font, left + 264, bottom - 50, 40, 18, Component.literal("Number")));
                numberBox.setMaxLength(2);
                numberBox.setHint(Component.literal("0-36"));
                if (rouletteBet.matches("\\d+")) numberBox.setValue(rouletteBet);
                numberBox.setResponder(v -> {
                    if (v.matches("\\d{1,2}") && Integer.parseInt(v) <= 36) rouletteBet = String.valueOf(Integer.parseInt(v));
                });
                game(Button.builder(Component.literal("Spin"), b -> play(a -> SkyBallsCasino.roulette(a, rouletteBet))).bounds(left + 8, by, 110, 20).build());
            }
            case SLOTS -> game(Button.builder(Component.literal("Spin"), b -> play(SkyBallsCasino::slots)).bounds(left + 8, by, 110, 20).build());
            case HILO -> {
                game(Button.builder(Component.literal("Start"), b -> play(a -> SkyBallsCasino.hilo("start", a))).bounds(left + 8, by, 80, 20).build());
                game(Button.builder(Component.literal("Higher"), b -> SkyBallsCasino.hilo("higher", 0)).bounds(left + 94, by, 80, 20).build());
                game(Button.builder(Component.literal("Lower"), b -> SkyBallsCasino.hilo("lower", 0)).bounds(left + 180, by, 80, 20).build());
                game(Button.builder(Component.literal("Cash out"), b -> SkyBallsCasino.hilo("cashout", 0)).bounds(left + 266, by, 86, 20).build());
            }
            case DAILY -> game(Button.builder(Component.literal("Claim daily"), b -> SkyBallsCasino.claimDaily()).bounds(width / 2 - 60, by, 120, 20).build());
            case LEADERBOARD -> {
                for (int i = 0; i < BOARDS.length; i++) {
                    String b = BOARDS[i];
                    Button button = addRenderableWidget(Button.builder(Component.literal(boardLabel(b)), btn -> {
                        board = b;
                        boardAskedAt = 0;
                        rebuildWidgets();
                    }).bounds(left + 8 + i * 87, top() + 20, 84, 18).build());
                    button.active = !b.equals(board);
                }
            }
            default -> {}
        }
    }

    private void game(Button button) {
        gameButtons.add(addRenderableWidget(button));
    }

    private void switchTo(Tab t) {
        if (t == Tab.BLACKJACK) {
            Minecraft.getInstance().gui.setScreen(new SkyBallsCasinoScreen());
            return;
        }
        tab = t;
        rebuildWidgets();
    }

    private interface Bet {
        void place(long amount);
    }

    private void play(Bet bet) {
        long amount;
        try {
            amount = Long.parseLong(betBox.getValue().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return;
        }
        if (amount < 1) return;
        lastBet = amount;
        bet.place(amount);
    }

    @Override
    public void tick() {
        SkyBallsCasino.ensureReady();
        SkyBallsCasino.tick();
        long now = System.currentTimeMillis();
        if (tab == Tab.STATS && now - statsAskedAt > 20_000L && SkyBallsCasino.loggedIn()) {
            statsAskedAt = now;
            SkyBallsCasino.requestStats();
        }
        if (tab == Tab.LEADERBOARD && now - boardAskedAt > 30_000L && SkyBallsCasino.loggedIn()) {
            boardAskedAt = now;
            SkyBallsCasino.requestBoard(board);
        }
        boolean ready = SkyBallsCasino.status().isEmpty() && !SkyBallsCasino.waiting && !animating();
        boolean enabled = SkyBallsCasino.gameEnabled(tab.game);
        SkyBallsCasino.HiLo hilo = SkyBallsCasino.hilo;
        boolean hiloPlaying = hilo != null && hilo.playing();
        for (Button b : gameButtons) {
            b.active = ready && enabled;
            String label = b.getMessage().getString();
            if (tab == Tab.HILO) {
                if (label.equals("Start")) b.active &= !hiloPlaying;
                else b.active &= hiloPlaying;
            }
            if (tab == Tab.DAILY) b.active = SkyBallsCasino.daily != null && SkyBallsCasino.daily.available() && enabled && !SkyBallsCasino.waiting;
        }
    }

    private boolean animating() {
        SkyBallsCasino.Result r = SkyBallsCasino.lastResult;
        return r != null && System.currentTimeMillis() - r.at() < ANIMATION_MS;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xB005070B);
        int left = left();
        int top = top();
        int right = left + W;
        int bottom = top + H;
        g.fill(left, top, right, bottom, FELT);
        g.outline(left, top, W, H, 0xFF6B4A1E);
        g.text(font, tab.label, left + 8, top + 8, GOLD, true);
        String money = SkyBallsCasino.balance < 0 ? "$..." : "$" + Sbc.number(SkyBallsCasino.balance);
        g.text(font, money, right - 8 - font.width(money), top + 8, GOLD, true);

        String status = SkyBallsCasino.status();
        if (!status.isEmpty()) {
            g.centeredText(font, status, width / 2, top + 90, TEXT);
        } else if (!SkyBallsCasino.gameEnabled(tab.game)) {
            g.centeredText(font, "This game is turned off right now.", width / 2, top + 90, 0xFFFF7F7F);
        } else {
            switch (tab) {
                case COINFLIP -> drawCoinflip(g, left, top);
                case DICE -> drawDice(g, left, top);
                case ROULETTE -> drawRoulette(g, left, top);
                case SLOTS -> drawSlots(g, left, top);
                case HILO -> drawHiLo(g, left, top);
                case DAILY -> drawDaily(g, left, top);
                case STATS -> drawStats(g, left, top);
                case LEADERBOARD -> drawBoard(g, left, top);
                default -> {}
            }
        }
        if (tab.ordinal() <= Tab.HILO.ordinal()) g.fill(left, bottom - 58, right, bottom - 57, FELT_DARK);

        String error = SkyBallsCasino.error;
        if (!error.isEmpty() && System.currentTimeMillis() - SkyBallsCasino.errorAt < 5_000L) {
            g.centeredText(font, error, width / 2, top + 22, 0xFFFF5555);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    // ------------------------------------------------------------------------------------------------ games

    /** The latest result for this game, once its animation has finished (null while spinning or if none). */
    private SkyBallsCasino.Result result(String game, boolean[] spinning) {
        SkyBallsCasino.Result r = SkyBallsCasino.lastResult;
        if (r == null || !r.game().equals(game)) return null;
        spinning[0] = System.currentTimeMillis() - r.at() < ANIMATION_MS;
        return r;
    }

    private void drawOutcome(GuiGraphicsExtractor g, SkyBallsCasino.Result r, int y) {
        String text;
        int colour;
        switch (r.outcome()) {
            case "won" -> {
                text = "You win! +$" + Sbc.number(r.payout() - r.bet());
                colour = 0xFF55FF55;
            }
            case "push" -> {
                text = "Push, your bet is back.";
                colour = 0xFFFFFF55;
            }
            default -> {
                text = "You lose. -$" + Sbc.number(r.bet());
                colour = 0xFFFF5555;
            }
        }
        g.centeredText(font, text, width / 2, y, colour);
    }

    private void drawCoinflip(GuiGraphicsExtractor g, int left, int top) {
        g.text(font, "Pick heads or tails. Pays 2x.", left + 8, top + 20, 0xFF7FBF97, false);
        boolean[] spinning = {false};
        SkyBallsCasino.Result r = result("coinflip", spinning);
        int cx = width / 2;
        int cy = top + 80;
        String side = r == null ? "" : firstString(r.detail(), "result", "side", "landed", "outcome");
        if (r != null && spinning[0]) {
            // The coin turns: its width shrinks and grows.
            float t = (System.currentTimeMillis() - r.at()) / 80f;
            int w = Math.max(2, Math.round(Math.abs((float) Math.cos(t)) * 28));
            g.fill(cx - w, cy - 28, cx + w, cy + 28, 0xFFD4A52A);
            g.fill(cx - Math.max(1, w - 3), cy - 25, cx + Math.max(1, w - 3), cy + 25, 0xFFF0C94A);
            return;
        }
        g.fill(cx - 28, cy - 28, cx + 28, cy + 28, 0xFFD4A52A);
        g.fill(cx - 25, cy - 25, cx + 25, cy + 25, 0xFFF0C94A);
        String face = side.isEmpty() ? "?" : side.toLowerCase().startsWith("h") ? "H" : "T";
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(3f, 3f);
        g.centeredText(font, face, 0, -4, 0xFF8A6412);
        g.pose().popMatrix();
        if (r != null) {
            g.centeredText(font, side.isEmpty() ? "" : "It landed on " + side + ".", width / 2, top + 116, TEXT);
            drawOutcome(g, r, top + 130);
        }
    }

    private void drawDice(GuiGraphicsExtractor g, int left, int top) {
        double multiplier = 99.0 / diceChance;
        g.text(font, "Win chance " + diceChance + "% (type 1-95) pays " + String.format("%.2fx", multiplier) + ".", left + 8, top + 20, 0xFF7FBF97, false);
        g.text(font, "Under: roll below " + diceChance + ". Over: roll above " + (100 - diceChance) + ".", left + 8, top + 32, MUTED, false);
        g.text(font, "Chance:", left + 222, top + H - 45, TEXT, false);
        boolean[] spinning = {false};
        SkyBallsCasino.Result r = result("dice", spinning);
        String roll;
        if (r == null) roll = "--.--";
        else if (spinning[0]) roll = String.format("%05.2f", random.nextDouble() * 99.99);
        else roll = String.format("%05.2f", r.detail().has("roll") ? r.detail().get("roll").getAsDouble() : 0);
        g.pose().pushMatrix();
        g.pose().translate(width / 2f, top + 72);
        g.pose().scale(4f, 4f);
        g.centeredText(font, roll, 0, 0, spinning[0] ? 0xFFCCCCCC : TEXT);
        g.pose().popMatrix();
        if (r != null && !spinning[0]) drawOutcome(g, r, top + 130);
    }

    private static String rouletteLabel(String bet) {
        return switch (bet) {
            case "red" -> "Red";
            case "black" -> "Black";
            case "odd" -> "Odd";
            case "even" -> "Even";
            case "low" -> "1-18";
            case "high" -> "19-36";
            case "dozen1" -> "1st 12";
            case "dozen2" -> "2nd 12";
            case "dozen3" -> "3rd 12";
            case "column1" -> "Col 1";
            case "column2" -> "Col 2";
            case "column3" -> "Col 3";
            default -> bet;
        };
    }

    private static int numberColour(int n) {
        if (n == 0) return 0xFF1E8A3A;
        for (int red : RED_NUMBERS) if (red == n) return 0xFFC8202C;
        return 0xFF15151A;
    }

    private void drawRoulette(GuiGraphicsExtractor g, int left, int top) {
        String bet = rouletteBet.matches("\\d+") ? "number " + rouletteBet + " (36x)" : rouletteLabel(rouletteBet)
            + (rouletteBet.startsWith("dozen") || rouletteBet.startsWith("column") ? " (3x)" : " (2x)");
        g.text(font, "Your bet: " + bet + ". Or type a number:", left + 8, top + 20, 0xFF7FBF97, false);
        g.text(font, "Number:", left + 222, top + H - 45, TEXT, false);
        boolean[] spinning = {false};
        SkyBallsCasino.Result r = result("roulette", spinning);
        int landed = r == null ? -1 : (int) Sbc.lng(r.detail(), "number", Sbc.lng(r.detail(), "result", -1));
        // The strip of numbers slides by, slowing down, and stops on the one the server rolled.
        int shown = landed;
        if (r != null && spinning[0]) {
            float t = (System.currentTimeMillis() - r.at()) / (float) ANIMATION_MS;
            shown = Math.floorMod(landed - Math.round((1 - t) * (1 - t) * 60), 37);
        }
        int cx = width / 2;
        int y = top + 60;
        for (int i = -3; i <= 3; i++) {
            int n = shown < 0 ? Math.floorMod(i, 37) : Math.floorMod(shown + i, 37);
            int x = cx + i * 30 - 13;
            g.fill(x, y, x + 26, y + 26, numberColour(n));
            if (i == 0) g.outline(x - 2, y - 2, 30, 30, GOLD);
            g.centeredText(font, String.valueOf(n), x + 13, y + 9, 0xFFFFFFFF);
        }
        if (r != null && !spinning[0]) drawOutcome(g, r, top + 100);
    }

    private ItemStack symbolStack(String symbol) {
        ItemStack stack = new ItemStack(SYMBOL_ITEMS.getOrDefault(symbol, Items.BARRIER));
        if (symbol.equals("SEVEN") || symbol.equals("DIAMOND")) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private void drawSlots(GuiGraphicsExtractor g, int left, int top) {
        g.text(font, "Three of a kind wins big. Seven and Diamond pay the most.", left + 8, top + 20, 0xFF7FBF97, false);
        boolean[] spinning = {false};
        SkyBallsCasino.Result r = result("slots", spinning);
        List<String> reels = new ArrayList<>();
        if (r != null) for (JsonElement e : Sbc.arr(r.detail(), "reels")) reels.add(e.getAsString());
        long elapsed = r == null ? Long.MAX_VALUE : System.currentTimeMillis() - r.at();
        int cx = width / 2;
        for (int i = 0; i < 3; i++) {
            int x = cx - 90 + i * 64;
            int y = top + 44;
            g.fill(x, y, x + 52, y + 60, 0xFFF5F1E6);
            g.outline(x, y, 52, 60, 0xFF6B4A1E);
            // Each reel stops a little after the one before it.
            boolean stopped = elapsed > ANIMATION_MS * (i + 2) / 4;
            String symbol = r == null || reels.size() <= i ? SYMBOLS[i + 2]
                : stopped ? reels.get(i) : SYMBOLS[(int) ((System.currentTimeMillis() / 70 + i * 3) % SYMBOLS.length)];
            g.pose().pushMatrix();
            g.pose().translate(x + 10, y + 8);
            g.pose().scale(2f, 2f);
            g.item(symbolStack(symbol), 0, 0);
            g.pose().popMatrix();
            g.centeredText(font, symbol.charAt(0) + symbol.substring(1).toLowerCase(), x + 26, y + 48, 0xFF333333);
        }
        if (r != null && elapsed > ANIMATION_MS) drawOutcome(g, r, top + 116);
    }

    private static String cardRank(int card) {
        return switch (card) {
            case 1 -> "A";
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            default -> String.valueOf(card);
        };
    }

    private void drawCard(GuiGraphicsExtractor g, int card, int x, int y, int w, int h, boolean faceDown) {
        if (faceDown || card < 1) {
            g.fill(x, y, x + w, y + h, 0xFF7A1420);
            g.outline(x, y, w, h, 0xFFEADFC8);
            return;
        }
        g.fill(x, y, x + w, y + h, 0xFFF5F1E6);
        g.outline(x, y, w, h, 0xFF8C8778);
        int colour = card % 2 == 0 ? 0xFFC8202C : 0xFF15151A;
        g.text(font, cardRank(card), x + 3, y + 3, colour, false);
        g.centeredText(font, card % 2 == 0 ? "♥" : "♠", x + w / 2, y + h / 2 - 4, colour);
    }

    private void drawHiLo(GuiGraphicsExtractor g, int left, int top) {
        g.text(font, "Higher wins on the same or higher card, lower on the same or lower.", left + 8, top + 20, 0xFF7FBF97, false);
        SkyBallsCasino.HiLo h = SkyBallsCasino.hilo;
        if (h == null) {
            g.centeredText(font, "Place a bet and press Start.", width / 2, top + 80, TEXT);
            return;
        }
        long elapsed = System.currentTimeMillis() - h.at();
        boolean flipping = elapsed < 500;
        int cx = width / 2;
        drawCard(g, h.card(), cx - 20, top + 40, 40, 56, flipping);
        // The cards so far.
        List<Integer> history = h.history();
        for (int i = 0; i < Math.min(8, history.size()); i++) {
            drawCard(g, history.get(history.size() - 1 - i), left + 8 + i * 22, top + 108, 18, 24, false);
        }
        g.text(font, "Bet $" + Sbc.number(h.bet()) + "   x" + String.format("%.2f", h.multiplier()) + "   Rounds: " + h.rounds(), left + 8, top + 138, TEXT, false);
        if (h.playing()) {
            g.text(font, "Higher x" + String.format("%.2f", h.higherMultiplier()), cx + 30, top + 50, 0xFF55FF55, false);
            g.text(font, "Lower x" + String.format("%.2f", h.lowerMultiplier()), cx + 30, top + 64, 0xFFFF7F7F, false);
            if (h.cashout() > 0) g.text(font, "Cash out: $" + Sbc.number(h.cashout()), cx + 30, top + 78, GOLD, false);
        } else if (!flipping) {
            String text = !h.message().isBlank() ? h.message()
                : "lost".equals(h.state()) ? "Wrong guess. -$" + Sbc.number(h.bet()) : "Cashed out $" + Sbc.number(h.payout()) + "!";
            g.centeredText(font, text, cx, top + 152, "lost".equals(h.state()) ? 0xFFFF5555 : 0xFF55FF55);
        }
    }

    private void drawDaily(GuiGraphicsExtractor g, int left, int top) {
        SkyBallsCasino.Daily d = SkyBallsCasino.daily;
        if (d == null) {
            g.centeredText(font, "Loading...", width / 2, top + 80, TEXT);
            return;
        }
        g.centeredText(font, "Daily streak: " + d.streak() + " day" + (d.streak() == 1 ? "" : "s"), width / 2, top + 30, GOLD);
        long[] rewards = {50, 75, 100, 150, 200, 300, 500};
        int x = width / 2 - rewards.length * 24;
        int current = d.available() ? d.streak() % rewards.length : Math.max(0, d.streak() - 1) % rewards.length;
        for (int i = 0; i < rewards.length; i++) {
            boolean done = i < current || (!d.available() && i == current);
            boolean next = d.available() && i == current;
            g.fill(x, top + 50, x + 44, top + 80, next ? 0xFF3B7B4B : done ? 0xFF2A4A34 : 0xFF1A2A20);
            g.outline(x, top + 50, 44, 30, next ? GOLD : 0xFF3B465B);
            g.centeredText(font, "Day " + (i + 1), x + 22, top + 54, MUTED);
            g.centeredText(font, "$" + rewards[i], x + 22, top + 66, done ? 0xFF55FF55 : TEXT);
            x += 48;
        }
        if (d.available()) {
            g.centeredText(font, "Your reward" + (d.reward() > 0 ? " of $" + Sbc.number(d.reward()) : "") + " is ready!", width / 2, top + 100, 0xFF55FF55);
        } else {
            long left2 = d.nextAt() - System.currentTimeMillis();
            g.centeredText(font, "Next reward in " + (left2 > 0 ? Sbc.duration(left2) : "a moment")
                + (d.tomorrow() > 0 ? " ($" + Sbc.number(d.tomorrow()) + ")" : ""), width / 2, top + 100, TEXT);
        }
        g.centeredText(font, "Resets every day at midnight UTC. Miss a day and the streak starts over.", width / 2, top + 116, MUTED);
    }

    private void drawStats(GuiGraphicsExtractor g, int left, int top) {
        JsonObject stats = SkyBallsCasino.stats;
        if (stats == null) {
            g.centeredText(font, "Loading...", width / 2, top + 80, TEXT);
            return;
        }
        JsonObject s = Sbc.obj(stats, "stats");
        long rank = Sbc.lng(stats, "rank", 0);
        String[][] rows = {
            {"Rank", rank > 0 ? "#" + rank : "-"},
            {"Games played", Sbc.number(Sbc.lng(s, "played", 0))},
            {"Won / lost / pushed", Sbc.lng(s, "won", 0) + " / " + Sbc.lng(s, "lost", 0) + " / " + Sbc.lng(s, "pushed", 0)},
            {"Wagered", "$" + Sbc.number(Sbc.lng(s, "wagered", 0))},
            {"Paid out", "$" + Sbc.number(Sbc.lng(s, "paidOut", 0))},
            {"Profit", "$" + Sbc.number(Sbc.lng(s, "paidOut", 0) - Sbc.lng(s, "wagered", 0))},
            {"Biggest win", "$" + Sbc.number(Sbc.lng(s, "biggestWin", 0))}};
        int y = top + 26;
        for (String[] row : rows) {
            g.text(font, row[0], left + 12, y, MUTED, false);
            g.text(font, row[1], left + 150, y, TEXT, false);
            y += 12;
        }
        JsonObject perGame = Sbc.obj(s, "games");
        int gx = left + 230;
        int gy = top + 26;
        g.text(font, "By game", gx, gy, GOLD, false);
        for (Map.Entry<String, JsonElement> e : perGame.entrySet()) {
            gy += 12;
            String value = e.getValue().isJsonObject() ? Sbc.lng(e.getValue().getAsJsonObject(), "played", 0) + " played" : e.getValue().toString();
            g.text(font, e.getKey() + ": " + value, gx, gy, TEXT, false);
            if (gy > top + H - 30) break;
        }
    }

    private static String boardLabel(String by) {
        return switch (by) {
            case "biggestWin" -> "Biggest win";
            case "wagered" -> "Wagered";
            case "streak" -> "Daily streak";
            default -> "Balance";
        };
    }

    private void drawBoard(GuiGraphicsExtractor g, int left, int top) {
        List<SkyBallsCasino.Entry> entries = SkyBallsCasino.BOARDS.getOrDefault(board, List.of());
        String self = Minecraft.getInstance().getUser().getName();
        if (entries.isEmpty()) g.centeredText(font, "Loading...", width / 2, top + 80, TEXT);
        int y = top + 46;
        for (int i = 0; i < Math.min(12, entries.size()); i++) {
            SkyBallsCasino.Entry e = entries.get(i);
            boolean you = e.username().equalsIgnoreCase(self);
            int colour = i == 0 ? GOLD : i == 1 ? 0xFFD8DEE9 : i == 2 ? 0xFFD08A4E : you ? 0xFF7FE0A0 : TEXT;
            g.text(font, (i + 1) + ". " + e.username(), left + 12, y, colour, false);
            String value = board.equals("streak") ? e.value() + " days" : "$" + Sbc.number(e.value());
            g.text(font, value, left + W - 12 - font.width(value), y, colour, false);
            y += 13;
        }
        long you = SkyBallsCasino.BOARD_YOU.getOrDefault(board, 0L);
        if (you > 12) g.text(font, "You: #" + you, left + 12, y + 4, 0xFF7FE0A0, false);
    }

    private static String firstString(JsonObject o, String... keys) {
        for (String key : keys) {
            String value = Sbc.str(o, key);
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
