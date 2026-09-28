package com.epic60869.skyballs.features.misc;

import com.epic60869.skyballs.SkyBallsConfig;
import com.epic60869.skyballs.custom.util.Compat;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

/**
 * Screenshot sharing, like Skysoft's: after F2, the "Saved screenshot as ..." message gets an [Upload] button. It
 * uploads the picture to Litterbox (catbox.moe's temporary file host, no account or key needed) and gives you the
 * link with [Send in /sbc] and [Copy Link]. The links are direct images, so they preview when hovered in chat.
 */
public final class ScreenshotShare {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public enum Host {
        LITTERBOX_1H("Litterbox (1 hour)", "1h"),
        LITTERBOX_12H("Litterbox (12 hours)", "12h"),
        LITTERBOX_24H("Litterbox (24 hours)", "24h"),
        LITTERBOX_72H("Litterbox (3 days)", "72h"),
        CATBOX("Catbox (permanent)", null);

        private final String label;
        final String time;

        Host(String label, String time) {
            this.label = label;
            this.time = time;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private ScreenshotShare() {}

    private static SkyBallsConfig.Misc config() {
        SkyBallsConfig c = SkyBallsConfig.current();
        return c == null ? null : c.misc;
    }

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            for (String root : Compat.COMMAND_ROOTS) {
                dispatcher.register(ClientCommands.literal(root).then(ClientCommands.literal("screenshot")
                    .then(ClientCommands.literal("upload").then(ClientCommands.argument("file", StringArgumentType.greedyString())
                        .executes(c -> upload(StringArgumentType.getString(c, "file")))))));
            }
        });
    }

    /** Adds the [Upload] button to Minecraft's "Saved screenshot as ..." message (called from the chat mixin). */
    public static Component decorate(Component message) {
        SkyBallsConfig.Misc c = config();
        if (message == null || c == null || !c.screenshotSharing) return message;
        if (!(message.getContents() instanceof TranslatableContents translatable) || !translatable.getKey().equals("screenshot.success")) return message;
        Object[] args = translatable.getArgs();
        if (args.length == 0 || !(args[0] instanceof Component file)) return message;
        String name = file.getString();
        if (!name.matches("[\\w .()-]+\\.png")) return message;
        Host host = c.screenshotHost == null ? Host.LITTERBOX_72H : c.screenshotHost;
        return message.copy().append(Component.literal("  [Upload]").withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
            .withClickEvent(new ClickEvent.RunCommand("/sb screenshot upload " + name))
            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Upload it and get a link to share in /sbc.\n").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("Uploads publicly to " + host + ": anyone with the link can see it.").withStyle(ChatFormatting.YELLOW))))));
    }

    private static int upload(String name) {
        Minecraft mc = Minecraft.getInstance();
        Path file = mc.gameDirectory.toPath().resolve("screenshots").resolve(name).normalize();
        if (!file.startsWith(mc.gameDirectory.toPath().resolve("screenshots")) || !Files.isRegularFile(file)) {
            return say(Component.literal("Couldn't find that screenshot.").withStyle(ChatFormatting.RED));
        }
        SkyBallsConfig.Misc c = config();
        Host host = c == null || c.screenshotHost == null ? Host.LITTERBOX_72H : c.screenshotHost;
        say(Component.literal("Uploading " + name + "...").withStyle(ChatFormatting.GRAY));
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                String link = sendWithRetry(file, host);
                MutableComponent message = Component.literal("Screenshot uploaded: ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(link).withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(link)))))
                    .append(Component.literal("  [Send in /sbc]").withStyle(s -> s.withColor(ChatFormatting.DARK_GREEN).withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/sbc " + link))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Post the link in SkyBalls chat")))))
                    .append(Component.literal("  [Copy Link]").withStyle(s -> s.withColor(ChatFormatting.YELLOW).withBold(true)
                        .withClickEvent(new ClickEvent.CopyToClipboard(link))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Copy the link")))));
                say(message);
            } catch (Exception e) {
                say(Component.literal("Couldn't upload the screenshot: " + reason(e, host)).withStyle(ChatFormatting.RED)
                    .append(Component.literal("  [Retry]").withStyle(s -> s.withColor(ChatFormatting.YELLOW).withBold(true)
                        .withClickEvent(new ClickEvent.RunCommand("/sb screenshot upload " + name))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("Try uploading it again.\n").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal("If " + host + " keeps failing, pick another host under Misc > Screenshot Upload Host.")
                                .withStyle(ChatFormatting.GRAY)))))));
            }
        });
        return 1;
    }

    /** Litterbox and Catbox often fail for a moment (HTTP 500, a timeout): try once more before giving up. */
    private static String sendWithRetry(Path file, Host host) throws Exception {
        try {
            return send(file, host);
        } catch (Exception first) {
            Thread.sleep(2_000);
            return send(file, host);
        }
    }

    /** Multipart upload to Litterbox (temporary) or Catbox (permanent); both answer with the file's link. */
    private static String send(Path file, Host host) throws Exception {
        String boundary = "----SkyBalls" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        field(body, boundary, "reqtype", "fileupload");
        if (host.time != null) field(body, boundary, "time", host.time);
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"fileToUpload\"; filename=\""
            + file.getFileName() + "\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(Files.readAllBytes(file));
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        String url = host.time != null ? "https://litterbox.catbox.moe/resources/internals/api.php" : "https://catbox.moe/user/api.php";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .header("User-Agent", "SkyBalls")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        String text = response.body().trim();
        if (response.statusCode() != 200 || !text.startsWith("https://")) {
            // Error pages are whole HTML documents: only a short plain-text answer is worth showing.
            boolean readable = !text.isEmpty() && text.length() <= 200 && !text.contains("<");
            throw new IllegalStateException(host + " answered HTTP " + response.statusCode()
                + (readable ? ": " + text : response.statusCode() >= 500 ? " (its server is having problems, try again in a bit)" : ""));
        }
        return text;
    }

    /** A one-line reason for chat. */
    private static String reason(Exception e, Host host) {
        if (e instanceof IllegalStateException && e.getMessage() != null) return e.getMessage();
        if (e instanceof java.net.http.HttpTimeoutException) return host + " took too long to answer";
        if (e instanceof java.io.IOException) return "couldn't reach " + host;
        return e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
    }

    private static void field(ByteArrayOutputStream body, String boundary, String name, String value) throws java.io.IOException {
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static int say(Component message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.gui.hud.getChat().addClientSystemMessage(
                Component.literal("[SB] ").withStyle(ChatFormatting.LIGHT_PURPLE).append(message));
        });
        return 1;
    }
}
