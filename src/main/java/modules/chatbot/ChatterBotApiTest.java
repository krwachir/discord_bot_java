package modules.chatbot;

import com.google.code.chatterbotapi.ChatterBot;
import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;

public class ChatterBotApiTest {
    
    public static void main(String[] args) throws Exception {
        ChatterBotFactory factory = new ChatterBotFactory();

       /* ChatterBot bot1 = factory.create(ChatterBotType.CLEVERBOT, CC3c13sKaHrkAdnw8DK1ZdsL4PA);
        ChatterBotSession bot1session = bot1.createSession();*/

        ChatterBot bot2 = factory.create(ChatterBotType.PANDORABOTS, "b0dafd24ee35a477");
        ChatterBotSession bot2session = bot2.createSession();
        String s = "Do you love me?";
        //System.out.println("bot1> " + s);

        String reply = bot2session.think(s);
        System.out.println("bot2> " + reply);
        
        s = "How are you?";
        reply = bot2session.think(s);
        System.out.println("bot2> " + reply);

        //s = bot1session.think(s);

//          
        
    }
}
