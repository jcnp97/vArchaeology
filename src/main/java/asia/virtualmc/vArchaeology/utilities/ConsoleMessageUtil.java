package asia.virtualmc.vArchaeology.utilities;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;

public class ConsoleMessageUtil {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static void sendConsoleMessage(String miniMessageText) {
        CommandSender console = Bukkit.getConsoleSender();
        Component message = miniMessage.deserialize(miniMessageText);
        console.sendMessage(message);
    }
}
