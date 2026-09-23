package com.example.fivelines;

import java.util.Locale;

/** Stable, local content definitions shared by the progression UI and daily mode. */
final class ProgressionCatalog {
    static final class DailyPuzzle {
        final String id;
        final String title;
        final String summary;
        final String target;
        final int[] layout;

        DailyPuzzle(String id, String title, String summary, String target, int[] layout) {
            this.id = id;
            this.title = title;
            this.summary = summary;
            this.target = target;
            this.layout = layout;
        }
    }

    static final class Medal {
        final String id;
        final String name;
        final int threshold;

        Medal(String id, String name, int threshold) {
            this.id = id;
            this.name = name;
            this.threshold = threshold;
        }
    }

    static final class AbilityCard {
        final String id;
        final String group;
        final String name;
        final String condition;
        final boolean equipment;

        AbilityCard(String id, String group, String name, String condition, boolean equipment) {
            this.id = id;
            this.group = group;
            this.name = name;
            this.condition = condition;
            this.equipment = equipment;
        }
    }

    private static final String[] PUZZLE_TITLES = {
            "像素爱心", "坦克阵地", "太空入侵", "复古蘑菇", "迷宫出口", "像素飞船", "经典吃豆路线",
            "砖墙突破", "方块落影", "街机手柄", "月球基地", "火箭发射", "像素幽灵", "像素皇冠",
            "幸运星", "复古街机", "八位音符", "像素花朵", "太空轨道", "地下迷宫", "炸药演习", "救援行动",
            "像素城堡", "海岛地图", "复古机器人", "星星收集", "像素蝴蝶", "经典赛车旗", "最后一条命", "终极街机厅"
    };

    private static final String[] PUZZLE_SUMMARIES = {
            "清理关键位置，完成爱心轮廓主题目标", "打通棋盘通道并完成指定消除", "限定步数内清除分散目标",
            "清除指定颜色棋子", "清出通路并完成目标连线", "清理飞船轮廓上的关键位置", "限步清除路线节点",
            "连续完成指定次数消除", "清除指定形状区域", "清理手柄轮廓上的目标位置", "使用白棋完成指定颜色连线",
            "限定步数内完成两次消除", "清除棋盘角落的目标棋子", "完成指定长度的连线", "单次消除达到指定数量",
            "连续两回合完成消除", "按目标完成两条线", "清理花瓣位置并完成中心区域目标", "使用白棋补足指定连线",
            "在棋盘空间受限时完成消除", "使用炸药清除指定区域", "打开被围住的关键位置", "清理外围目标后完成指定连线",
            "分区清除指定颜色棋子", "完成一条横线和一条竖线", "达成指定消除数并保留步数", "完成两侧目标区域的清理",
            "清出棋盘中央通道", "使用少量步数完成指定连线", "综合使用白棋、炸药和普通消除"
    };

    private static final String[] PUZZLE_TARGETS = {
            "清除爱心轮廓上的 8 个目标位置", "完成 2 次横向连线", "12 步内清除 3 个角落目标", "清除 5 个红色棋子",
            "完成 1 条长度至少 5 的连线", "清除飞船中央 6 个位置", "10 步内清除路线节点", "连续消除 3 次",
            "清除中央方块区域", "清除手柄两侧目标", "使用白棋完成 1 条连线", "12 步内完成 2 次消除",
            "清除四角目标", "完成长度至少 6 的连线", "单次消除至少 7 个棋子", "连续两回合消除",
            "完成 2 条不同方向的线", "清除花瓣并完成中心连线", "使用白棋补足 1 条线", "空间受限时完成 1 次消除",
            "炸药影响 4 个目标位置", "打开棋盘中央通道", "清除外围后完成 1 条线", "清除两种颜色区域",
            "完成横线和竖线各 1 条", "消除 12 个棋子并保留 3 步", "清除左右两侧目标", "中央连续 5 格为空",
            "8 步内完成 1 条连线", "各使用白棋与炸药 1 次"
    };

    static final Medal[] MEDALS = {
            new Medal("medal_01", "初学之星", 0), new Medal("medal_02", "青铜新手", 100),
            new Medal("medal_03", "青铜行者", 250), new Medal("medal_04", "白银棋手", 500),
            new Medal("medal_05", "白银连线师", 900), new Medal("medal_06", "黄金棋手", 1400),
            new Medal("medal_07", "黄金大师", 2100), new Medal("medal_08", "铂金战略家", 3000),
            new Medal("medal_09", "铂金大师", 4200), new Medal("medal_10", "钻石宗师", 5800),
            new Medal("medal_11", "星耀传奇", 7800), new Medal("medal_12", "五线传说", 10000)
    };

    private static final String[][] CARD_ROWS = {
            {"连线启程", "初次连线", "首次完成消除", "五连新手", "首次完成五连", "清场见习", "单次消除至少 8 枚棋子", "连线熟手", "单局完成 5 次消除", "横线专精", "累计横向消除 10 次", "竖线专精", "累计纵向消除 10 次", "斜线巧手", "累计斜向消除 10 次", "连线收藏家", "累计消除 100 枚棋子"},
            {"分数里程", "百分达人", "单局达到 100 分", "双百分", "单局达到 200 分", "三百分", "单局达到 300 分", "千分起步", "单局达到 1,000 分", "高分挑战者", "单局达到 2,500 分", "高分专家", "单局达到 5,000 分", "稳步得分", "连续 5 次消除均获得分数", "终局逆转", "棋盘空间紧张时完成有效消除"},
            {"特殊棋子", "白棋初用", "首次使用白棋完成消除", "万能补位", "累计使用白棋补足连线 10 次", "炸药初用", "首次引爆炸药", "精准爆破", "一次炸药影响多个棋子", "双宝连携", "同一局使用白棋和炸药", "危机拆弹", "利用炸药解除棋盘拥堵", "工具达人", "累计使用特殊棋子 20 次", "特殊大师", "同一局完成白棋连线和炸药消除"},
            {"连击策略", "连击初体验", "首次连续回合完成消除", "双连击", "连续 2 回合完成消除", "三连击", "连续 3 回合完成消除", "步步为营", "连续 10 步后仍完成有效消除", "空间整理", "成功清理关键通道并完成消除", "先手布局", "查看棋子预告后完成消除", "多线规划", "单步产生多条有效消除", "棋盘掌控", "完成指定高密度棋盘挑战"},
            {"每日挑战", "每日来客", "完成 1 次每日挑战", "七日挑战者", "累计完成 7 次每日挑战", "残局新秀", "完成 5 种不同残局", "残局专家", "完成 15 种不同残局", "三十关征服者", "完成首期全部 30 关", "无提示通关", "不使用提示完成任意残局", "步数大师", "按关卡要求保留指定步数通关", "全勤记录", "完成一个自然月的每日挑战"},
            {"坚持成长", "初次开局", "完成首局游戏", "十局玩家", "累计完成 10 局", "百局玩家", "累计完成 100 局", "逆境坚持", "棋盘拥堵时仍成功完成消除", "复盘学徒", "使用撤销后完成本局", "新纪录", "刷新个人最高分", "连日归来", "连续游玩 3 天", "长线收藏家", "累计解锁 20 张能力卡"},
            {"主题收藏", "像素之心", "完成像素爱心残局", "街机记忆", "完成任意 5 个复古主题残局", "太空漫游", "完成全部太空主题残局", "拆弹专家", "完成全部炸药主题残局", "彩图鉴赏家", "完成 20 个不同主题残局", "五线全明星", "集齐其余 53 张能力卡"}
    };

    static final AbilityCard[] CARDS = buildCards();

    private ProgressionCatalog() { }

    static DailyPuzzle puzzleForDay(int dayOfYear) {
        int index = Math.floorMod(dayOfYear - 1, PUZZLE_TITLES.length);
        return new DailyPuzzle(
                String.format(Locale.US, "daily_%02d", index + 1),
                PUZZLE_TITLES[index],
                PUZZLE_SUMMARIES[index],
                PUZZLE_TARGETS[index],
                createLayout(index)
        );
    }

    private static int[] createLayout(int index) {
        int[] layout = new int[81];
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                boolean marked = ((row + col + index) % 4 == 0)
                        || (row == index % 7 && col >= 2 && col <= 6)
                        || (col == (index * 3) % 7 + 1 && row >= 2 && row <= 6);
                if (marked) {
                    layout[row * 9 + col] = ((index + row + col + 2) % 8) + 1;
                }
            }
        }
        layout[(index * 7) % 81] = 9; // white wildcard is part of the authored position
        layout[(index * 11 + 20) % 81] = 10; // bomb position is deterministic
        return layout;
    }

    private static AbilityCard[] buildCards() {
        AbilityCard[] result = new AbilityCard[54];
        int cursor = 0;
        for (String[] row : CARD_ROWS) {
            String group = row[0];
            for (int i = 1; i + 1 < row.length; i += 2) {
                boolean equipment = row[i].equals("先手布局") || row[i].equals("复盘学徒");
                result[cursor] = new AbilityCard(
                        String.format(Locale.US, "card_%02d", cursor + 1),
                        group,
                        row[i],
                        row[i + 1],
                        equipment
                );
                cursor++;
            }
        }
        return result;
    }
}
