package me.wiefferink.areashop.tools;

import me.wiefferink.interactivemessenger.generators.TellrawGenerator;
import me.wiefferink.interactivemessenger.parsers.YamlParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LanguageConverter {

    public static void performConversion(ConfigurationNode root) throws ConfigurateException {
        if (!root.isMap()) {
            convertNode(root);
            return;
        }
        Map<Object, ? extends ConfigurationNode> map = root.childrenMap();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : map.entrySet()) {
            ConfigurationNode node = entry.getValue();
            if (node.isMap()) {
                performConversion(node);
            } else {
                convertNode(node);
            }
        }
    }

    public static void performConversion(File toConvert, File output) throws IOException {
        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .source(() -> new BufferedReader(new FileReader(toConvert, StandardCharsets.UTF_8)))
                .sink(() -> new BufferedWriter(new FileWriter(output, StandardCharsets.UTF_8)))
                .headerMode(HeaderMode.PRESERVE)
                .nodeStyle(NodeStyle.BLOCK)
                .build();
        ConfigurationNode root = loader.load();
        performConversion(root);
        loader.save(root);
    }

    public static List<String> convertRawList(List<String> messages) {
        List<String> jsonMessages = TellrawGenerator.generate(YamlParser.parse(messages));
        List<String> converted = new ArrayList<>();
        for (String json : jsonMessages) {
            BaseComponent[] bungeeComponents = ComponentSerializer.parse(json);
            Component convertedComponent = BungeeComponentSerializer.get()
                    .deserialize(bungeeComponents);
            String miniMessage = MiniMessage.miniMessage()
                    .serialize(convertedComponent);
            converted.add(miniMessage);
        }
        return converted;
    }

    private static void convertNode(ConfigurationNode node) throws ConfigurateException {
        boolean isList = node.isList();
        List<String> messages;
        if (isList) {
            messages = node.getList(String.class, Collections.emptyList());
        } else {
            messages = Collections.singletonList(node.getString());
        }
        List<String> converted = convertRawList(messages);
        if (converted.size() == 1) {
            node.set(String.class, converted.getFirst());
        } else {
            node.setList(String.class, converted);
        }
    }


}
