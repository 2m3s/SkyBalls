package com.epic60869.skyballs;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** /sb casino: the blackjack table, with the SkyBalls leaderboard (most money first) beside it. */
public final class SkyBallsCasinoScreen extends Screen {
    private static final int TABLE_W = 300;
    private static final int BOARD_W = 140;
    private static final int GAP = 8;
    private static final int HEIGHT = 230;
    private static final int CARD_W = 26;
    private static final int CARD_H = 36;

    private static final int FELT = 0xFF0F4D2E;
    private static final int FELT_DARK = 0xFF0A3620;
    private static final int PANEL = 0xFF121722;
    private static final int BORDER = 0xFF3B465B;
    private static final int GOLD = 0xFFFFD35A;
    private static final int TEXT = 0xFFF3F6FF;
    private static final int MUTED = 0xFF9AA5B8;

    private static long lastBet = 10;

    private EditBox betBox;
    private Button deal;
    private Button hit;
    private Button stand;
    private Button doubleDown;
    private final Button[] chips = new Button[4];
    private long leaderboardAt;

    public SkyBallsCasinoScreen() {
        super(Component.literal("SkyBalls Casino"));
    }

    private int left() {
        return (width - (TABLE_W + GAP + BOARD_W)) / 2;
    }

    private int top() {
        return (height - HEIGHT) / 2;
    }

    @Override
    protected void init() {
        int left = left();
        int bottom = top() + HEIGHT;

        String bet = betBox == null ? String.valueOf(lastBet) : betBox.getValue();
        betBox = new EditBox(font, left + 8, bottom - 50, 60, 18, Component.literal("Bet"));
        betBox.setMaxLength(12);
        betBox.setValue(bet);
        addRenderableWidget(betBox);

        long[] amounts = {10, 25, 100, -1};
        for (int i = 0; i < chips.length; i++) {
            long amount = amounts[i];
            chips[i] = addRenderableWidget(Button.builder(Component.literal(amount < 0 ? "All in" : "$" + amount),
                    b -> betBox.setValue(String.valueOf(amount < 0 ? Math.max(0, SkyBallsCasino.balance) : amount)))
                .bounds(left + 72 + i * 44, bottom - 51, amount < 0 ? 46 : 42, 20).build());
        }

        deal = addRenderableWidget(Button.builder(Component.literal("Deal"), b -> deal())
            .bounds(left + 8, bottom - 27, 90, 20).build());
        hit = addRenderableWidget(Button.builder(Component.literal("Hit"), b -> SkyBallsCasino.action("hit"))
            .bounds(left + 8, bottom - 27, 90, 20).build());
        stand = addRenderableWidget(Button.builder(Component.literal("Stand"), b -> SkyBallsCasino.action("stand"))
            .bounds(left + 104, bottom - 27, 90, 20).build());
        doubleDown = addRenderableWidget(Button.builder(Component.literal("Double"), b -> SkyBallsCasino.action("double"))
            .bounds(left + 200, bottom - 27, 92, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> SkyBallsCasino.refreshLeaderboard())
            .bounds(left + TABLE_W + GAP + 8, bottom - 27, BOARD_W - 16, 20).build());
    }

    private void deal() {
        long amount;
        try {
            amount = Long.parseLong(betBox.getValue().replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return;
        }
        if (amount < 1) return;
        lastBet = amount;
        SkyBallsCasino.bet(amount);
    }

    @Override
    public void tick() {
        SkyBallsCasino.ensureReady();
        // Keep the leaderboard fresh while the table is open.
        long now = System.currentTimeMillis();
        if (now - leaderboardAt > 30_000L) {
            leaderboardAt = now;
            SkyBallsCasino.refreshLeaderboard();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xB005070B);
        int left = left();
        int top = top();
        int right = left + TABLE_W;
        int bottom = top + HEIGHT;

        // The table.
        g.fill(left, top, right, bottom, FELT);
        outline(g, left, top, right, bottom, 0xFF6B4A1E);
        g.fill(left, bottom - 58, right, bottom, FELT_DARK);
        g.text(font, "Blackjack", left + 8, top + 8, GOLD, true);
        String money = SkyBallsCasino.balance < 0 ? "$..." : "$" + format(SkyBallsCasino.balance);
        g.text(font, money, right - 8 - font.width(money), top + 8, GOLD, true);
        g.text(font, "Blackjack pays 3:2 · Dealer stands on 17", left + 8, top + 20, 0xFF7FBF97, false);

        String status = SkyBallsCasino.status();
        SkyBallsCasino.Hand hand = SkyBallsCasino.hand;
        boolean ready = status.isEmpty();
        boolean playing = ready && hand != null && hand.playing();

        if (!ready) {
            g.centeredText(font, status, (left + right) / 2, top + 90, TEXT);
        } else if (hand == null) {
            g.centeredText(font, "Place a bet and press Deal.", (left + right) / 2, top + 90, TEXT);
        } else {
            drawRow(g, "Dealer", hand.dealer(), hand.dealerTotal(), left + 8, top + 36);
            drawRow(g, "You", hand.player(), hand.playerTotal(), left + 8, top + 36 + CARD_H + 20);
            g.text(font, "Bet $" + format(hand.bet()), right - 8 - font.width("Bet $" + format(hand.bet())), top + 36 + CARD_H + 20, TEXT, false);
            if (!hand.playing()) {
                String result = hand.message().isEmpty() ? resultText(hand) : hand.message();
                g.centeredText(font, result, (left + right) / 2, bottom - 70, resultColour(hand.state()));
            }
        }

        if (ready && SkyBallsCasino.balance == 0 && !playing) {
            String refill = SkyBallsCasino.refillAt > System.currentTimeMillis()
                ? "You're broke! Free $100 in " + duration(SkyBallsCasino.refillAt - System.currentTimeMillis()) + "."
                : "You're broke! Your free $100 is on its way.";
            g.centeredText(font, refill, (left + right) / 2, bottom - 70, 0xFFFF7F7F);
        }

        String error = SkyBallsCasino.error;
        if (!error.isEmpty() && System.currentTimeMillis() - SkyBallsCasino.errorAt < 5_000L) {
            g.centeredText(font, error, (left + right) / 2, top + 22 + 10, 0xFFFF5555);
        }

        // Only the buttons that make sense right now.
        boolean idle = ready && !playing && !SkyBallsCasino.waiting;
        deal.visible = !playing;
        deal.active = idle && SkyBallsCasino.balance > 0;
        deal.setMessage(Component.literal(hand == null ? "Deal" : "Deal again"));
        betBox.setEditable(!playing);
        for (Button chip : chips) chip.active = !playing;
        hit.visible = stand.visible = doubleDown.visible = playing;
        hit.active = stand.active = !SkyBallsCasino.waiting;
        doubleDown.active = !SkyBallsCasino.waiting && hand != null && hand.canDouble();

        drawLeaderboard(g, right + GAP, top, bottom);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawRow(GuiGraphicsExtractor g, String who, List<String> cards, int total, int x, int y) {
        String label = who + (total > 0 ? " (" + total + ")" : "");
        g.text(font, label, x, y, TEXT, true);
        for (int i = 0; i < cards.size(); i++) drawCard(g, cards.get(i), x + i * (CARD_W + 4), y + 11);
    }

    private void drawCard(GuiGraphicsExtractor g, String card, int x, int y) {
        if (card.equals("??") || card.length() < 2) {
            g.fill(x, y, x + CARD_W, y + CARD_H, 0xFF7A1420);
            outline(g, x, y, x + CARD_W, y + CARD_H, 0xFFEADFC8);
            outline(g, x + 3, y + 3, x + CARD_W - 3, y + CARD_H - 3, 0xFFB0303C);
            return;
        }
        String rank = card.substring(0, card.length() - 1);
        char suit = card.charAt(card.length() - 1);
        String symbol = switch (suit) {
            case 'H' -> "♥";
            case 'D' -> "♦";
            case 'C' -> "♣";
            default -> "♠";
        };
        int colour = suit == 'H' || suit == 'D' ? 0xFFC8202C : 0xFF15151A;
        g.fill(x, y, x + CARD_W, y + CARD_H, 0xFFF5F1E6);
        outline(g, x, y, x + CARD_W, y + CARD_H, 0xFF8C8778);
        g.text(font, rank, x + 3, y + 3, colour, false);
        g.centeredText(font, symbol, x + CARD_W / 2, y + CARD_H / 2 - 1, colour);
    }

    private void drawLeaderboard(GuiGraphicsExtractor g, int x, int top, int bottom) {
        int right = x + BOARD_W;
        g.fill(x, top, right, bottom, PANEL);
        outline(g, x, top, right, bottom, BORDER);
        g.text(font, "Leaderboard", x + 8, top + 8, GOLD, true);
        String self = Minecraft.getInstance().getUser().getName();
        List<SkyBallsCasino.Entry> entries = SkyBallsCasino.leaderboard;
        if (entries.isEmpty()) {
            g.text(font, "No players yet.", x + 8, top + 26, MUTED, false);
        }
        int y = top + 24;
        for (int i = 0; i < Math.min(10, entries.size()); i++) {
            SkyBallsCasino.Entry e = entries.get(i);
            boolean you = e.username().equalsIgnoreCase(self);
            int colour = i == 0 ? GOLD : i == 1 ? 0xFFD8DEE9 : i == 2 ? 0xFFD08A4E : you ? 0xFF7FE0A0 : TEXT;
            String name = (i + 1) + ". " + e.username();
            String amount = "$" + shortAmount(e.balance());
            g.text(font, trim(name, BOARD_W - 22 - font.width(amount)), x + 8, y, colour, false);
            g.text(font, amount, right - 8 - font.width(amount), y, colour, false);
            y += 12;
        }
        if (SkyBallsCasino.yourRank > 10) {
            g.text(font, "You: #" + SkyBallsCasino.yourRank, x + 8, y + 4, 0xFF7FE0A0, false);
        }
    }

    private String trim(String text, int width) {
        if (font.width(text) <= width) return text;
        while (text.length() > 1 && font.width(text + "…") > width) text = text.substring(0, text.length() - 1);
        return text + "…";
    }

    private static String resultText(SkyBallsCasino.Hand hand) {
        return switch (hand.state()) {
            case "blackjack" -> "Blackjack! +$" + format(hand.payout() - hand.bet());
            case "won" -> "You win! +$" + format(hand.payout() - hand.bet());
            case "push" -> "Push, your bet is back.";
            default -> "Dealer wins. -$" + format(hand.bet());
        };
    }

    private static int resultColour(String state) {
        return switch (state) {
            case "blackjack", "won" -> 0xFF55FF55;
            case "push" -> 0xFFFFFF55;
            default -> 0xFFFF5555;
        };
    }

    private static String format(long amount) {
        return String.format(Locale.ENGLISH, "%,d", amount);
    }

    private static String shortAmount(long amount) {
        if (amount >= 1_000_000_000L) return String.format(Locale.ENGLISH, "%.1fb", amount / 1e9);
        if (amount >= 1_000_000L) return String.format(Locale.ENGLISH, "%.1fm", amount / 1e6);
        if (amount >= 10_000L) return String.format(Locale.ENGLISH, "%.1fk", amount / 1e3);
        return format(amount);
    }

    private static String duration(long ms) {
        long minutes = Math.max(1, ms / 60_000L);
        return minutes >= 60 ? minutes / 60 + "h " + minutes % 60 + "m" : minutes + "m";
    }

    private static void outline(GuiGraphicsExtractor g, int left, int top, int right, int bottom, int colour) {
        g.fill(left, top, right, top + 1, colour);
        g.fill(left, bottom - 1, right, bottom, colour);
        g.fill(left, top, left + 1, bottom, colour);
        g.fill(right - 1, top, right, bottom, colour);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
