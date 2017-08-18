package modules.chatbot;

import com.google.code.chatterbotapi.*;

public class ChatterBotApiTest {
    
    public static void main(String[] args) throws Exception {
        ChatterBotFactory factory = new ChatterBotFactory();
        ChatterBot bot2 = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
        ChatterBotSession bot2session = bot2.createSession();

        String s = "Hi";
        while (true) {

            System.out.println("bot1> " + s);

            s = bot2session.think(s);
            System.out.println("bot2> " + s);
        }
    }
}