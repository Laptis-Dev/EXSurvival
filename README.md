# EXSurvival

一个功能完整的 Minecraft Paper 服务器小游戏插件，提供开局战和生存竞技模式。

## 功能特性

### 核心游戏模式

#### 开局战阶段
- **武器选择系统**：6种可配置的武器套装
  - 下界合金矛（锋利 II、突进 II）
  - 下界合金斧（击退 II）
  - 重锤（风爆 I + 风弹）
  - 弓（力量 II + 箭）
  - 三叉戟（忠诚、穿刺 II）
  - 弩（快速装填 III、力量 III + 箭）

- **标准防具装备**：完全可配置的下界合金套装
- **世界边界限制**：可配置的 PvP 竞技区域
- **排名系统**：按死亡顺序自动排名
- **生存奖励**：前三名获得不死图腾和末影珍珠

#### 正式游戏阶段
- **自由生存时间**：30分钟无 PvP 发育期
- **PvP 竞技**：90分钟激烈对战
- **死亡计数**：死亡5次自动淘汰
- **出生点保护**：防止玩家在出生点被击杀
- **最终排名**：游戏结束时显示完整排名

### 系统功能

- **多语言支持**：中文和英文，自动根据玩家语言显示（支持尚不完整）
- **玩家数据保存**：自动保存和恢复玩家背包、位置、游戏模式
- **离线处理**：玩家离线自动淘汰，重新加入时恢复状态
- **记分板显示**：实时显示死亡次数和排名
- **Boss 栏显示**：游戏进度和剩余时间实时显示
- **倒计时系统**：各阶段自动倒计时提醒
- **权限系统**：细粒度的权限控制

## ⚙️ 配置

### config.yml 主要配置项

```yaml
# 开局战配置
opening-battle:
  world-border-size: 100  # 世界边界大小（正方形边长）

  # 武器套装配置
  weapon-kits:
    netherite-spear:
      name: "下界合金矛"
      item: NETHERITE_SPEAR
      enchantments:
        sharpness: 2
        knockback: 2

  # 标准防具配置
  standard-armor:
    helmet:
      type: NETHERITE_HELMET
      enchantments:
        protection: 4
        unbreaking: 3
    # ... 其他防具

  # 生存奖励配置
  survival-rewards:
    rank-1:
      items:
        - type: TOTEM_OF_UNDYING
          amount: 1
        - type: ENDER_PEARL
          amount: 3

# 正式游戏配置
survival-phase:
  max-health: 40              # 玩家最大血量
  free-survival-time: 30      # 自由生存时间（分钟）
  total-game-time: 120        # 总游戏时间（分钟）
  max-deaths: 5               # 死亡次数限制
  difficulty: easy            # 游戏难度
  tp-time: 8                  # 决斗时间（最后 X 分钟）
  spawn-protection: 16        # 出生点保护半径
```

### 自定义武器套装

在 `config.yml` 中的 `opening-battle.weapon-kits` 部分添加新武器：

```yaml
custom-sword:
  name: "自定义剑"
  item: DIAMOND_SWORD
  enchantments:
    sharpness: 3
    fire_aspect: 2
  lore:
    - "§7锋利 III"
    - "§7火焰附加 II"
  extra-items:
    - type: GOLDEN_APPLE
      amount: 5
```

## 命令

### 主命令

所有命令都可以使用 `/exsurvival` 或缩写 `/exs`

#### 开始游戏
```
/exs start <世界ID>
```
- **权限**：`exsurvival.control`
- **描述**：在指定世界开始新的游戏
- **示例**：`/exs start world`

#### 停止游戏
```
/exs stop
```
- **权限**：`exsurvival.control`
- **描述**：立即停止当前运行的游戏，恢复所有玩家
- **示例**：`/exs stop`

#### 重载配置
```
/exs reload
```
- **权限**：`exsurvival.reload`
- **描述**：重载插件配置文件和语言文件
- **示例**：`/exs reload`

### Tab 补全

所有命令都支持 Tab 补全：
- `/exs` → 显示可用子命令
- `/exs start` → 显示所有可用世界

## 权限

| 权限                 | 描述               | 默认值 |
| -------------------- | ------------------ | ------ |
| `exsurvival.control` | 允许开始和停止游戏 | OP     |
| `exsurvival.reload`  | 允许重载配置       | OP     |

## 游戏流程

### 阶段 1：武器选择（5-10分钟）

1. 所有在线玩家自动加入游戏
2. 玩家打开"EXSurvival 说明书"选择武器
3. 选择后点击"确认准备"按钮
4. 所有玩家准备后自动进入开局战

**玩家操作**：
- 右键点击说明书打开菜单
- 点击武器选项选择
- 点击绿色羊毛确认准备

### 阶段 2：开局战（5-15分钟）

1. 所有玩家在世界出生点出生
2. 装备防具和选定的武器
3. 5秒倒计时后开启 PvP
4. 玩家在 100x100 区域内进行大乱斗
5. 死亡玩家进入观战模式
6. 最后一名玩家存活时游戏结束

**排名规则**：
- 最后死亡的玩家 = 第一名
- 倒数第二死亡的玩家 = 第二名
- 以此类推

**奖励分发**：
- 第一名：不死图腾 x1 + 末影珍珠 x3
- 第二名：不死图腾 x1 + 末影珍珠 x2
- 第三名：不死图腾 x1 + 末影珍珠 x1
- 其他玩家：铁锭 x3

### 阶段 3：正式游戏（120分钟）

#### 自由生存（0-30分钟）
- 无 PvP，玩家可自由发育
- 血量上限设置为 40
- 玩家可采集资源、建造、合成

#### PvP 竞技（30-120分钟）
- 开启 PvP，玩家可互相攻击
- 死亡计数开始记录
- 死亡 5 次自动淘汰
- 记分板显示死亡次数

#### 决斗时间（112-120分钟）
- 最后 8 分钟所有玩家传送回出生点
- 出生点保护激活（16格范围内无法破坏方块）
- 进行最后的决斗

### 阶段 4：游戏结束

1. 显示最终排名
2. 恢复所有玩家的原始数据
3. 传送玩家回原位置
4. 恢复原游戏模式

## 许可证

本项目采用 MIT 许可证。详见 LICENSE 文件。

