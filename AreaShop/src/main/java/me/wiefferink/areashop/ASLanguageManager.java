package me.wiefferink.areashop;

import java.io.Reader;
import java.nio.charset.StandardCharsets;

import me.wiefferink.interactivemessenger.Log;
import me.wiefferink.interactivemessenger.processing.Message;
import me.wiefferink.interactivemessenger.source.LanguageManager;
import me.wiefferink.interactivemessenger.translation.Transifex;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Collections;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;


public class ASLanguageManager extends LanguageManager {
    private JavaPlugin plugin;
    private String jarLanguagePath;
    private File languageFolder;
    private String wgPrefix;
    private List<String> chatPrefix;
    private Map<String, List<String>> currentLanguage, defaultLanguage;

    public ASLanguageManager(JavaPlugin plugin,
                             String jarLanguagePath,
                             String currentLanguageName,
                             String defaultLanguageName,
                             List<String> chatPrefix,
                             String wgPrefix) {
        super(plugin, jarLanguagePath, currentLanguageName, defaultLanguageName, chatPrefix);
        this.plugin = plugin;
        this.jarLanguagePath = jarLanguagePath;
        this.chatPrefix = chatPrefix;
        this.languageFolder = new File(plugin.getDataFolder() + File.separator + jarLanguagePath);

        Message.init(this, plugin.getLogger());
        saveDefaults();
        currentLanguage = loadLanguage(currentLanguageName);
        if (defaultLanguageName.equals(currentLanguageName)) {
            defaultLanguage = currentLanguage;
        } else {
            defaultLanguage = loadLanguage(defaultLanguageName);
        }
        this.wgPrefix = wgPrefix;
    }

    @Override
    public List<String> getMessage(String key) {
        List<String> message;
        if (key.equalsIgnoreCase(Message.CHATLANGUAGEVARIABLE)) {
            message = chatPrefix;
        } else if (key.equalsIgnoreCase("wgPrefix")) {
            message = new ArrayList<>();
            message.add(wgPrefix);
        } else if (currentLanguage.containsKey(key)) {
            message = currentLanguage.get(key);
        } else {
            message = defaultLanguage.get(key);
        }
        if (message == null) {
            Log.warn("Did not find message '" + key + "' in the current or default language");
            return new ArrayList<>();
        }
        return new ArrayList<>(message);
    }

    private void saveDefaults() {
        // Create the language folder if it not exists
        if (!languageFolder.exists()) {
            if (!languageFolder.mkdirs()) {
                Log.warn("Could not create language directory: " + languageFolder.getAbsolutePath());
                return;
            }
        }
        File jarPath;
        try {
            // Read jar as ZIP file
            jarPath = new File(plugin.getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        } catch (URISyntaxException e) {
            Log.error("Failed to find location of jar file:", ExceptionUtils.getStackTrace(e));
            return;
        }
        try (ZipFile jar = new ZipFile(jarPath)) {
            Enumeration<? extends ZipEntry> entries = jar.entries();

            // Each entry is a file or directory
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                // Filter to YAML files in the language directory
                if (!entry.isDirectory() && entry.getName()
                        .startsWith(jarLanguagePath + "/") && entry.getName()
                        .endsWith(".yml")) {
                    // Save the file to disk
                    File targetFile = new File(languageFolder.getAbsolutePath() + File.separator + entry.getName()
                            .substring(entry.getName().lastIndexOf("/")));
                    try (
                            InputStream input = jar.getInputStream(entry);
                            OutputStream output = new FileOutputStream(targetFile)
                    ) {
                        byte[] bytes = input.readAllBytes();
                        output.write(bytes);
                    } catch (IOException e) {
                        Log.warn("Something went wrong saving a default language file: " + targetFile.getAbsolutePath());
                    }
                }
            }
        } catch (
                IOException e) {
            Log.error("Failed to read zip file:", ExceptionUtils.getStackTrace(e));
        }
    }

    private Map<String, List<String>> loadLanguage(String key) {
        return loadLanguage(key, true);
    }

    /**
     * Loads the specified language
     *
     * @param key     The language to load
     * @param convert try conversion or not (infinite recursion prevention)
     * @return Map with the messages loaded from the file
     */
    private Map<String, List<String>> loadLanguage(String key, boolean convert) {
        Map<String, List<String>> result = new HashMap<>();

        // Load the language file
        boolean convertFromTransifex = false;
        File file = new File(languageFolder.getAbsolutePath() + File.separator + key + ".yml");
        try (
                InputStream inputStream = new FileInputStream(file);
                Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        ) {
            // Detect empty language files, happens when the YAML parsers prints an exception (it does return an empty YamlConfiguration though)
            YamlConfiguration ymlFile = YamlConfiguration.loadConfiguration(reader);
            if (ymlFile.getKeys(false).isEmpty()) {
                Log.warn("Language file " + key + ".yml has zero messages.");
                return result;
            }

            // Retrieve the messages from the YAML file and create the result
            if (convert && Transifex.needsConversion(ymlFile)) {
                convertFromTransifex = true;
            } else {
                for (String messageKey : ymlFile.getKeys(false)) {
                    List<String> toPut;
                    if (ymlFile.isList(messageKey)) {
                        toPut = ymlFile.getStringList(messageKey);
                    } else {
                        toPut = Collections.singletonList(ymlFile.getString(messageKey));
                    }
                    result.put(messageKey, new ArrayList<>(toPut));
                }
            }
        } catch (IOException e) {
            Log.warn("Could not load language file: " + file.getAbsolutePath());
        }

        // Do conversion (after block above closed the reader)
        if (convertFromTransifex) {
            if (!Transifex.convertFrom(file)) {
                Log.warn("Failed to convert " + file.getName() + " from the Transifex layout to the AreaShop layout, check the errors above");
            }
            return loadLanguage(key, false);
        }

        return result;
    }
}
