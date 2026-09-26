package com.epic60869.skyballs;

import com.epic60869.skyballs.custom.util.Compat;
import com.epic60869.skyballs.mixin.SkyBallsContainerScreenAccessor;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds the real-world date to SkyBlock calendar tooltips. Ported from Skyblocker's
 * DateCalculatorTooltip (LGPL-3.0), including its use of Fabric's tooltip event so it
 * works alongside other mods that change tooltip rendering.
 */
public final class SkyBallsDateCalculator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("E MMM d yyyy HH:mm", Locale.US).withZone(ZoneId.systemDefault());
    private static final TimeProvider[] PROVIDERS = {new Calendar()};
    private static TimeProvider currentTimer;

    private SkyBallsDateCalculator() {}

    public static void init() {
        // Look at whatever screen is open when the tooltip is built, rather than relying on a
        // particular container screen class or its hovered slot, so it also works when another
        // mod replaces or wraps Hypixel's menus.
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            Screen screen = Minecraft.getInstance().gui.screen();
            if (screen == null) return;
            currentTimer = timerFor(screen);
            addToTooltip(stack, lines);
        });
    }

    private static TimeProvider timerFor(Screen screen) {
        String screenTitle = ChatFormatting.stripFormatting(screen.getTitle().getString()).trim();
        for (TimeProvider timer : PROVIDERS) {
            if (timer.test(screenTitle)) return timer;
        }
        return null;
    }

    /**
     * Adds the real-world date to a container item's tooltip. Called from Fabric's tooltip event and again from
     * SkyBallsContainerTooltipMixin (the menu's own tooltip), so it still works when another mod's tooltip listener
     * fails before ours runs; the date is only added once.
     */
    public static List<Component> addDates(ItemStack stack, List<Component> lines) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (screen == null) return lines;
        for (Component line : lines) {
            if (line.getStyle().isItalic() && DATE_LINE.matcher(line.getString()).matches()) return lines;
        }
        List<Component> out = new java.util.ArrayList<>(lines);
        currentTimer = timerFor(screen);
        addToTooltip(stack, out);
        return out;
    }

    private static final Pattern DATE_LINE = Pattern.compile("^[A-Z][a-z]{2} [A-Z][a-z]{2} \\d{1,2} \\d{4} \\d{2}:\\d{2}$");

    /** "Starts in: 1d 2h 3m", "Ends in: 5m 10s", "Starts in: 2 days 3 hours": the countdown after "in". */
    private static final Pattern COUNTDOWN_LINE = Pattern.compile("(?:Starts|Ends|Begins|Ending|Starting) in:? (?<time>.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PART = Pattern.compile("(?<n>\\d+) ?(?<unit>days?|d|hours?|hrs?|h|minutes?|mins?|m|seconds?|secs?|s)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DAY_NUMBER = Pattern.compile("(?:^|\\D)(?<day>\\d{1,2})(?:st|nd|rd|th)?(?:\\D|$)");

    private static void addToTooltip(ItemStack stack, List<Component> lines) {
        SkyBallsConfig config = SkyBallsConfig.current();
        if (config == null || !config.misc.calendarTimeToRealTime || !Compat.isOnSkyblock() || lines.isEmpty()) return;

        // 1. Countdowns ("Starts in: 1d 2h") in any SkyBlock menu: now + the time left, like Skyblocker's Events.
        boolean added = false;
        for (int i = 1; i < lines.size(); i++) {
            Instant instant = countdown(ChatFormatting.stripFormatting(lines.get(i).getString()));
            if (instant == null) continue;
            lines.add(++i, dateLine(instant));
            added = true;
        }
        if (added) return;

        // 2. A SkyBlock date written in the tooltip ("Late Spring 12th, Year 412"), in any menu.
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = WRITTEN_DATE.matcher(ChatFormatting.stripFormatting(lines.get(i).getString()));
            if (!m.find()) continue;
            int month = Calendar.MONTHS.indexOf(m.group("month").toLowerCase(Locale.ROOT));
            int day = Integer.parseInt(m.group("day"));
            int year = m.group("year") != null ? Integer.parseInt(m.group("year"))
                : currentTimer instanceof Calendar calendar ? calendar.year : currentYear();
            if (month < 0 || day < 1 || day > 31 || year < 1) continue;
            lines.add(i + 1, dateLine(SkyBallsSkyblockTime.toRealWorld(year, month, day).toInstant()));
            return;
        }

        // 3. A day in a month view ("Early Spring, Year 412"): Hypixel shows the day as the stack size, as Skyblocker
        // reads it. Only for items whose name has that day number, so buttons and filler never get a made-up date.
        if (currentTimer instanceof Calendar calendar) {
            int day = stack.getCount();
            String name = ChatFormatting.stripFormatting(lines.getFirst().getString());
            Matcher m = DAY_NUMBER.matcher(name);
            boolean named = false;
            while (m.find()) if (Integer.parseInt(m.group("day")) == day) named = true;
            if (!named || day < 1 || day > 31) return;
            lines.add(1, dateLine(SkyBallsSkyblockTime.toRealWorld(calendar.year, calendar.monthIndex, day).toInstant()));
        }
    }

    /** The real time a "Starts in: ..." line points to, rounded to the minute, or null. */
    private static Instant countdown(String line) {
        if (line == null) return null;
        Matcher m = COUNTDOWN_LINE.matcher(line);
        if (!m.find()) return null;
        long seconds = 0;
        boolean any = false;
        Matcher part = TIME_PART.matcher(m.group("time"));
        while (part.find()) {
            long n = Long.parseLong(part.group("n"));
            char unit = Character.toLowerCase(part.group("unit").charAt(0));
            seconds += switch (unit) {
                case 'd' -> n * 86400;
                case 'h' -> n * 3600;
                case 'm' -> n * 60;
                default -> n;
            };
            any = true;
        }
        if (!any) return null;
        return Instant.now().plusSeconds(seconds + 30).truncatedTo(ChronoUnit.MINUTES);
    }

    private static final Pattern WRITTEN_DATE = Pattern.compile("(?<month>(?:Early |Late )?(?:Spring|Summer|Autumn|Winter)) (?<day>\\d{1,2})(?:st|nd|rd|th)?(?:,? Year (?<year>\\d+))?");

    private static int currentYear() {
        long elapsed = System.currentTimeMillis() - SkyBallsSkyblockTime.SKYBLOCK_EPOCH.toEpochMilli();
        return (int) (elapsed / SkyBallsSkyblockTime.YEAR_MILLIS) + 1;
    }

    private static Component dateLine(Instant instant) {
        return Component.literal(DATE_FORMATTER.format(instant)).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY);
    }

    private interface TimeProvider {
        boolean test(String screenTitle);

        Instant getStartTime(ItemStack stack, String qualifiedLine);
    }

    private static class Calendar implements TimeProvider {
        private static final Pattern PATTERN = Pattern.compile("(?<month>(?:Early |Late )?(?:Spring|Summer|Autumn|Winter)),? Year (?<year>\\d+)");
        static final List<String> MONTHS = List.of(
            "early spring", "spring", "late spring", "early summer", "summer", "late summer",
            "early autumn", "autumn", "late autumn", "early winter", "winter", "late winter");
        private int monthIndex;
        private int year = 1;

        @Override
        public boolean test(String screenTitle) {
            Matcher matcher = PATTERN.matcher(screenTitle);
            if (!matcher.find()) return false;
            int maybeMonth = MONTHS.indexOf(matcher.group("month").trim().toLowerCase(Locale.ROOT));
            if (maybeMonth < 0) return false;
            int maybeYear;
            try {
                maybeYear = Integer.parseInt(matcher.group("year"));
            } catch (NumberFormatException e) {
                return false;
            }
            if (maybeYear == 0) return false;
            monthIndex = maybeMonth;
            year = maybeYear;
            return true;
        }

        @Override
        public Instant getStartTime(ItemStack stack, String qualifiedLine) {
            if (stack.getCount() < 1 || stack.getCount() > 31) return null;
            return SkyBallsSkyblockTime.toRealWorld(year, monthIndex, stack.getCount()).toInstant();
        }
    }
}
