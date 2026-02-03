# 目标
现在请根据这个项目模板实现这个 EXSurvival Minecraft Paper 插件，Gradle 项目已经配置完成，Paper API 版本 1.21.11-R0.1-SNAPSHOT
# 模板
```java
package xyz.lapismc.exsurvival;

import net.minecraft.SharedConstants;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

@DefaultQualifier(NonNull.class)
public class EXSurvivalPlugin extends JavaPlugin implements Listener {
    // ...
}

```
# 需求

## 综述

EXSurvival 插件是一个小游戏插件。

## 插件兼容性

考虑与以下插件的兼容性：
```
ℹ Server Plugins (28):
Paper Plugins (2):
 - JEIRecipeBridge, PlugManX
Bukkit Plugins (26):
 - AuthMe, EcoPower, Essentials, EssentialsChat, EssentialsSpawn, izhanLibPlugin, LiteXpansion, LuckPerms, ManhuntX
 MotdEngine, Multiverse-Core, SkinsRestorer, Slimefun, SlimeGlue, rilFools, ViaBackwards, ViaRewind
 ViaVersion, visual-crafting, voicechat, WorldEdit, WorldEditSlimefun, xaero-map-spigot
```

## 指令

以`exsurvival`为命名空间，有`/exsurvival:exsurvival`指令与其缩写`/exsurvival:exs`。

## 全球化

需要考虑全球化，以中文为默认语言。

## 游戏

有权限`exsurvival.control`者可使用 `/exs start <目标世界 ID>` 开始全服游戏。

### 开局战

首先保存并清除所有参与游戏的玩家的背包物品，保存其原坐标，切换为生存模式。待游戏结束后归还其所有物品，并将其传送回原坐标，恢复原游戏模式。

在消息栏向每位玩家提示简要的游戏规则。向玩家显示 `Title` “开局战”

每人可通过给予的一本`EXSurvival 说明书`打开游戏菜单，从下列基础武器套装中选择一种（默认配置，需可在`exsurvival.yml`中配置）：
 - 下界合金矛：锋利 II、突进 II
 - 下界合金斧：击退 II
 - 重锤：风爆 I，配有一组半风弹
 - 弓：力量 II，配有一组箭
 - 三叉戟：忠诚、穿刺 II
 - 弩：快速装填 III、力量 III，配有一组箭

每人选择好装备，并在菜单中确认准备后，开始开局战。所有玩家在目标世界默认出生点出生（出生在一起），在 100x100（默认配置，需可在`exsurvival.yml`中配置）的世界边界内，倒数 5 秒后，进行 PvP 大乱斗。以死亡次序为排序标准，最后死亡的为第一名，倒数第二死亡的为第二名，以此类推。死亡的玩家进入观战模式，并保存其装备。场上只剩下一名玩家后，复活所有玩家，恢复其死前装备，关闭 PvP，并根据以下四等额外分发生存奖励（默认配置，分发的物品需可在`exsurvival.yml`中配置）：
 - 第一名：不死图腾x1 末影珍珠x3
 - 第二名：不死图腾x1 末影珍珠x2
 - 第三名：不死图腾x1 末影珍珠x1
 - 其余玩家：铁锭x3

> 特殊情况：若最后两名玩家同归于尽，则共同算作第一名。这两名玩家后的玩家正常从第二名开始计算。

生存奖励分发后在消息栏进行提示。15 秒后进入下一环节。

### 正式游戏

将每人重新传送回目标世界默认出生点，关闭 PvP，倒数 5 秒后，进入自由生存时间。

设置全部玩家血量上限为 40，并回满血量。

在消息栏向每位玩家提示简要的游戏规则。向玩家显示 `Title` “自由生存”。

随后在 BOSS 栏显示当前阶段和剩余时间。

开局 30 分钟（默认配置，需可在`exsurvival.yml`中配置）发育，不得 PvP。

30 分钟后开启 PvP，向玩家显示 `Title` “PvP 开启”，并开始记录玩家死亡次数，同时显示在记分板上。死亡 5 次（默认配置，需可在`exsurvival.yml`中配置）则失败。

两小时（默认配置，需可在`exsurvival.yml`中配置）后回到出生点，关闭 PvP，显示游戏排名。
