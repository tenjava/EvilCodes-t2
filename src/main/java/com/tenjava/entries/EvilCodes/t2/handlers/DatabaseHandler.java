package com.tenjava.entries.EvilCodes.t2.handlers;

import com.tenjava.entries.EvilCodes.t2.utils.DBCore;
import com.tenjava.entries.EvilCodes.t2.utils.MySQLCore;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class DatabaseHandler {

    private static DBCore connection;
    private static String prefix;

    /*
    Table structure for ..._player: id, name, uuid, kills, deaths, firstlogin, lastlogin, mobkills
     */

    //Setup a databaseconnection with credentials from the config.yml
    public static void setup() {
        //Get all values
        DatabaseHandler.connection = new MySQLCore(FilesHandler.getConfig().getString("database.host"), FilesHandler.getConfig().getString("database.database"),
                FilesHandler.getConfig().getString("database.username"), FilesHandler.getConfig().getString("database.password"));
        DatabaseHandler.prefix = FilesHandler.getConfig().getString("database.prefix");
        if (connection.checkConnection()) {
            //Create table if not exists
            if (!connection.existsTable(prefix + "player")) {
                LogHandler.log("Connected to database!");
                LogHandler.log("Creating database table " + prefix + "player!");
                connection.execute("CREATE TABLE IF NOT EXISTS `" + prefix + "player` (" +
                        "  `id` int(11) NOT NULL AUTO_INCREMENT," +
                        "  `name` varchar(32) NOT NULL," +
                        "  `uuid` varchar(64) NOT NULL," +
                        "  `kills` int(11) NOT NULL," +
                        "  `deaths` int(11) NOT NULL," +
                        "  `firstlogin` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "  `lastlogin` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "  `mobkills` int(11) NOT NULL," +
                        "  PRIMARY KEY (`id`)" +
                        ") ENGINE=InnoDB  DEFAULT CHARSET=latin1 AUTO_INCREMENT=2 ;");
            }
        } else {
            //Connection failed
            LogHandler.err("Could not connect to database!");
        }
    }

    /**
     * Is this user recognized in the database?
     * @param player The player
     * @return boolean if player is in database
     */
    public static boolean userExists(final Player player) {
        final String ifquery = "SELECT * FROM `" + prefix + "player" + "` WHERE `uuid` = '" + player.getUniqueId().toString() + "';";
        return MySQLCore.mysqlExists(ifquery);
    }

    /**
     * Insert the player into the databse with default values
     * @param player The player
     */
    public static void insert(final Player player) {
        final Date today = new Date();
        final String query = "INSERT INTO `" + prefix + "player` (`id`, `name`, `uuid`, `kills`, `deaths`, `firstlogin`, `lastlogin`, `mobkills`) VALUES (NULL, '" + player.getName() + "', '" + player.getUniqueId().toString()
                + "', '0', '0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '0');";
        connection.execute(query);
    }

    /**
     * Get a value from a row (like kills, deaths, mobkills) ONLY INTEGER!
     * @param row Name of the value you want to get
     * @param player The player
     * @return int for the value
     */
    public static int getValue(final String row, final Player player) {
        final String query = "SELECT * FROM `" + prefix + "player" + "` WHERE `uuid` = '" + player.getUniqueId().toString() + "';";
        final ResultSet resultSet = connection.select(query);
        int value = 0;
        if (resultSet != null) {
            try {
                while (resultSet.next()) {
                    value = resultSet.getInt(row);
                }
            } catch (final SQLException ex) {
                LogHandler.err("Error while trying to get value from database for player " + player.getName());
                LogHandler.stackErr(ex);
            }
        } else {
            LogHandler.err("Cannot get " + row + " for " + player.getName() + ": Resultset is null!");
        }
        return value;
    }

    /**
     * Increase the value for the player with 1 (+1)
     * @param row Name of the value you want to increase
     * @param player The player
     */
    public static void increaseValue(final String row, final Player player) {
        final int newvalue = getValue(row, player) + 1;
        final String query = "UPDATE `" + prefix + "player` SET `" + row + "` = '" + newvalue + "' WHERE `uuid` = '" + player.getUniqueId().toString() + "';";
        connection.execute(query);
    }

    public static DBCore getConnection() {
        return DatabaseHandler.connection;
    }

    public static String getPrefix() {
        return DatabaseHandler.prefix;
    }
}
