/**
 * Spigot MC Inventory Tweaks plugin:<br>
 * A plugin aimed at providing minor, inventory-related tweaks to improve the
 * Minecraft experience<br><br>
 * <h3>Commands</h3>
 * <pre>
 * /sort    Sort a targeted chest or shulker box or the senders inventory if
 *          aforementioned blocks aren't targeted
 * /magnet  Toggle magnetism for the sender. When magnetism is enabled, all
 *          items within a 16x16x16 cube around the player that can be picked
 *          up are teleported to the player relatively frequently
 * /search  Find a given item type in any nearby chest and open the chest for
 *          the player
 * </pre>
 * <h3>Features</h3>
 * <ul>
 *     <li>Sneak + right-click inventory with sword to trigger /sort</li>
 *     <li>Auto-replace emptied stack in hand if more stacks of this type are available in a players inventory</li>
 * </ul>
 */
package dev.w1zzrd.invtweaks;