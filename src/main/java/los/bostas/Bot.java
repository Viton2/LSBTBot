package los.bostas;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Bot {

    public static void main(String[] args) {

        try (InputStream input = Bot.class.getClassLoader().getResourceAsStream("config.properties")) {

            Properties prop = new Properties();

            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                return;
            }

            //load a properties file from class path, inside static method
            prop.load(input);

            //get the property value and print it out
            JDA bot = JDABuilder.createDefault(prop.getProperty("api.config.token"))
                .setActivity(Activity.listening("sua mae gemendo"))
                .build();

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}
