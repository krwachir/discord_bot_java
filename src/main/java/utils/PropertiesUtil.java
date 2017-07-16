package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import sx.blah.discord.handle.obj.IGuild;

public class PropertiesUtil {
	private static final String GUILD_CONFIG_PATH = Main.Bot.USER_PATH + "/guildConfigs";
	
	Properties properties;
	String propertiesPath;
	public boolean fileExists = true;

	public PropertiesUtil(String propertiesPath) {
		this.propertiesPath = propertiesPath;
		
		File file = new File(propertiesPath);
		if (!file.exists()) {
			fileExists = false;
			return;
		}
		
		properties = new Properties();
		try {
			//config.load(new FileReader(new File(USER_PATH + "/config.properties")));
			Reader reader = new InputStreamReader(new FileInputStream(propertiesPath),"UTF-8");
			properties.load(reader);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}

	public String get(String key) {
		return properties.getProperty(key);
	}

	public void createPropFile(String guildFileName) {
		try {
			properties.store(new FileOutputStream(guildFileName), null);
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
	}

	public void removeProperty(String propName) {
		properties.remove(propName);
	}
	
	public void setProperty(String propName, String value) {
		properties.setProperty(propName, value);
	}
	
	public static void store(Properties properties, String path) {
		try {
			File propFile = new File(path);
			if (!(propFile).getParentFile().exists())
				(propFile).getParentFile().mkdirs();
			if (!propFile.exists()) {
				FileWriter writer = new FileWriter(propFile);
				properties.store(writer, "host settings");
			    writer.close();
			} else {
				properties.store(new FileOutputStream(propFile), null);
			}
		} catch (Exception e) {e.printStackTrace();}
			
	}

	public static String getGuildConfig(IGuild guild, String key) {
		String path = GUILD_CONFIG_PATH + "/" + guild.getStringID() + ".properties";
		Properties guildProperties = read(path);
		return guildProperties.getProperty(key);
	}

	public static void removeGuildProperty(IGuild guild, String key) {
		String path = GUILD_CONFIG_PATH + "/" + guild.getStringID() + ".properties";
		Properties guildProperties = read(path);
		guildProperties.remove(key);
		PropertiesUtil.store( guildProperties, path);
		
	}

	public static void setGuildProperty(IGuild guild, String key, String value) {
		String path = GUILD_CONFIG_PATH + "/" + guild.getStringID() + ".properties";
		Properties guildProperties = read(path);
		guildProperties.setProperty(key, value);
		PropertiesUtil.store( guildProperties, path);
		
	}
	
	private static Properties read(String propertiesPath) {
		Properties properties = new Properties();
		try {
			
			//config.load(new FileReader(new File(USER_PATH + "/config.properties")));
			Reader reader = new InputStreamReader(new FileInputStream(propertiesPath),"UTF-8");
			properties.load(reader);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return properties;
	}

}
