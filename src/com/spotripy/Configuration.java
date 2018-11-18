package com.spotripy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to control getting and setting of user configurations.
 *
 * @author Emmanuel
 */
public class Configuration {

    /**
     * The location where our configuration file will be saved.
     */
    public static final String CONFIG_DIRECTORY = System.getProperty("user.dir");

    /**
     * The name of our properties file.
     */
    public static final String CONFIG_FILENAME = "spotripy.config";

    /**
     * The configuration instance
     */
    private static Configuration instance;

    /**
     * The properties for this application.
     */
    private final Properties props;

    /**
     * The properties file object.
     */
    private static final File configFile = new File(CONFIG_DIRECTORY, CONFIG_FILENAME);

    /**
     * The Logger instance. All log messages from this class are routed through this
     * member.
     */
    private static final Logger logger = Logger.getLogger(Configuration.class.getName());

    /**
     * Creates instance of this class with default values.
     */
    private Configuration() {
        props = loadProperties();
    }

    /**
     * Gets the singleton instance of configuration.
     * 
     * @return the Configuration object
     */
    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    /**
     * Get a specific property
     *
     * @param name the name of the property to return
     * @return the value of the property specified in name
     */
    public String getProperty(String name) {
        return props.getProperty(name);
    }

    /**
     * Get a specific property
     *
     * @param name         the name of the property to return
     * @param defaultValue the default value if property is not set
     * @return the value of the property specified in name
     */
    public String getProperty(String name, String defaultValue) {
        return props.getProperty(name, defaultValue);
    }

    /**
     * Set a configuration property of name to the specified value. Then save the
     * configuration.
     *
     * @param name  the property name
     * @param value the property value
     */
    public void setProperty(String name, String value) {
        props.setProperty(name, value);
        // saveProperties();
    }

    /**
     * Load the application properties from the configuration file.
     *
     * @return the loaded properties
     */
    private Properties loadProperties() {
        Properties loadedProps = new Properties();
        if (configFile.exists() && configFile.canRead()) {
            synchronized (configFile) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    loadedProps.load(fis);
                    fis.close();
                } catch (FileNotFoundException ex) {
                    logger.log(Level.SEVERE, "File not found after existence verfied.", ex);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "File data could not be read.", ex);
                }
            }
        }
        return loadedProps;
    }

    /**
     * Save the application properties in the configuration file.
     */
    protected void saveProperties() {
        try (FileOutputStream fos = new FileOutputStream(configFile);) {
            if (configFile.canWrite()) {
                synchronized (configFile) {
                    if (configFile.exists()) {
                        configFile.delete();
                    }
                    configFile.createNewFile();
                    props.store(fos, "Spotripy Configuration");
                }
                fos.close();
            } else {
                throw new IOException("Can not write to config file");
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String toString() {
        return props.toString();
    }

}
