package com.example.fivelines;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {
    private SoundEngine soundEngine;
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xff101820);
        getWindow().setNavigationBarColor(0xfff1eadf);
        soundEngine = new SoundEngine(this);
        gameView = new GameView(this, soundEngine, savedInstanceState);
        setContentView(gameView);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (gameView != null) {
            gameView.saveState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (gameView != null) {
            gameView.invalidate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.onHostResume();
        }
    }

    @Override
    protected void onPause() {
        if (soundEngine != null) {
            soundEngine.stopMusic();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (soundEngine != null) {
            soundEngine.release();
        }
        super.onDestroy();
    }

    static final class GameView extends View {
        private static final int SIZE = 9;
        private static final int EMPTY = 0;
        private static final int NORMAL_COLORS = 8;
        private static final int COLOR_PINK = 5;
        private static final int COLOR_CYAN = 6;
        private static final int WHITE = 9;
        private static final int BOMB = 10;
        private static final int PREVIEW_COUNT = 3;
        private static final int BASE_SPAWN_COUNT = 3;
        private static final int MUSIC_TRACK_SYNTH = 0;
        private static final int MUSIC_TRACK_MIDI = 1;
        private static final int MUSIC_TRACK_SY = 2;
        private static final int MUSIC_TRACK_MOLLY = 3;
        private static final int MUSIC_TRACK_RAY = 4;
        private static final int EASTER_NONE = 0;
        private static final int EASTER_RAY = 1;
        private static final int EASTER_MOLLY = 2;
        private static final int EASTER_TOUTOU = 3;

        private static final int TOP_HEIGHT = 156;
        private static final long SPAWN_DURATION_MS = 320L;
        private static final long REMOVE_DURATION_MS = 500L;
        private static final long EASTER_FLASH_HALF_MS = 180L;
        private static final long EASTER_FLASH_FULL_MS = EASTER_FLASH_HALF_MS * 2L;
        private static final long EASTER_LOADING_DURATION_MS = EASTER_FLASH_FULL_MS * NORMAL_COLORS;
        private static final float BOARD_MARGIN = 16f;
        private static final float DEFAULT_DIFFICULTY = 1.1f;
        private static final float DEFAULT_WHITE_PROBABILITY = 0.8f;
        private static final float DEFAULT_BOMB_PROBABILITY = 0.4f;
        private static final int TOUTOU_LOADING_COLORS = 7;

        private static final String SETTINGS_PREFS = "five_lines_settings";
        private static final String SCORES_PREFS = "five_lines_scores";
        private static final String HISTORY_PREFS = "five_lines_history";
        private static final String ACHIEVEMENT_PREFS = "five_lines_achievements";

        private final Context context;
        private final SoundEngine sound;
        private final SharedPreferences settings;
        private final int[][] board = new int[SIZE][SIZE];
        private final int[] preview = new int[PREVIEW_COUNT];
        private final Random random = new Random();
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF menuBounds = new RectF();
        private final RectF shineBounds = new RectF();
        private final Bitmap[] carLogoBitmaps = new Bitmap[10];

    private int score;
    // Snapshot before a player move. A score change means that move cleared a line.
        private int removedCount;
        private int moveCount;
        private int lineClearCount;
        private int bestClearCount;
        private int currentChain;
        private int bestChain;
        private int bombsTriggered;
        private int[][] undoBoard;
        private int[] undoPreview;
        private int undoScore;
        private int undoRemovedCount;
        private int undoMoveCount;
        private int undoLineClearCount;
        private int undoBestClearCount;
        private long undoDailyRandomState;
        private int undoBestChain;
        private int undoBombsTriggered;
        private boolean undoAvailable;
        private boolean dailyChallenge;
        private long dailyRandomState;
        private int dailyBestScore;
        private int tutorialStep = -1;
        private int tutorialSource = -1;
        private int tutorialTarget = -1;
        private final RectF tutorialActionBounds = new RectF();
        private String achievementToast;
        private long achievementToastUntil;
        private boolean gameRecorded;
        private int selectedRow = -1;
        private int selectedCol = -1;
        private boolean moving;
        private boolean spawning;
        private boolean removing;
        private boolean gameOver;
        private boolean dialogShowing;
    private boolean spawnedThisTurn;
    // A successful line removal consumes the turn: resolve cascades, but do not spawn.
    private boolean clearedThisTurn;
        private int moveToken;

        private int movingType = EMPTY;
        private ArrayList<int[]> movePath = new ArrayList<>();
        private long moveStart;
        private long moveDuration;

        private long spawnStart;
        private final boolean[][] spawnedCells = new boolean[SIZE][SIZE];

        private long removalStart;
        private boolean[][] pendingRemove;
        private boolean[][] pendingLine;
        private boolean[][] pendingBlast;
        private final ArrayList<int[]> pendingBombs = new ArrayList<>();
        private int pendingLineCount;
        private int pendingBlastCount;
        private int pendingLineScore;

        private float boardLeft;
        private float boardTop;
        private float cellSize;
        private float boardSize;
        private int statusInset;
        private int navigationInset;

        private float difficultyMultiplier;
        private float whiteProbabilityMultiplier;
        private float bombProbabilityMultiplier;
        private boolean musicEnabled;
        private boolean soundEnabled;
        private float musicVolume;
        private float soundVolume;
        private int musicTrack;
        private int movementSpeed;
        private boolean adminMode;
        private boolean rayRacerMode;
        private boolean kuromiTheme;
        private boolean heartMode;
        private int easterKind = EASTER_NONE;
        private int claimedEaster = EASTER_NONE;
        private boolean easterLoading;
        private int easterSecretTaps;
        private long easterStart;
        private AlertDialog easterDialog;
        private int settingsMusicToggleCount;
        private boolean adminUnlockedByMusic;
        private final ArrayList<int[]> racerTrailCells = new ArrayList<>();
        private int racerTrailType = EMPTY;
        private long racerTrailUntil;

        GameView(Context context, SoundEngine sound, Bundle savedState) {
            super(context);
            this.context = context;
            this.sound = sound;
            settings = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
            loadSettings();
            setFocusable(true);
            if (savedState != null && savedState.containsKey("five_lines_board")) {
                restoreState(savedState);
            } else {
                resetGame();
                if (!settings.getBoolean("tutorial_seen", false)) {
                    postDelayed(this::beginTutorial, 450L);
                }
            }
        }

        void onHostResume() {
            sound.updateMusic(musicEnabled, musicVolume, activeMusicTrack());
        }

        private int activeMusicTrack() {
            if (heartMode) return MUSIC_TRACK_SY;
            if (claimedEaster == EASTER_MOLLY) return MUSIC_TRACK_MOLLY;
            if (claimedEaster == EASTER_RAY) return MUSIC_TRACK_RAY;
            return musicTrack;
        }

        private void loadSettings() {
            adminMode = settings.getBoolean("admin_mode", false);
            difficultyMultiplier = clamp(settings.getFloat("difficulty", DEFAULT_DIFFICULTY), 1f, 2f);
            whiteProbabilityMultiplier = clamp(settings.getFloat("white_probability", DEFAULT_WHITE_PROBABILITY), 0f, 2f);
            bombProbabilityMultiplier = clamp(settings.getFloat("bomb_probability", DEFAULT_BOMB_PROBABILITY), 0f, 2f);
            musicEnabled = settings.getBoolean("music_enabled", true);
            soundEnabled = settings.getBoolean("sound_enabled", true);
            musicVolume = clamp(settings.getFloat("music_volume", 0.35f), 0f, 1f);
            soundVolume = clamp(settings.getFloat("sound_volume", 0.75f), 0f, 1f);
            musicTrack = Math.max(
                    MUSIC_TRACK_SYNTH,
                    Math.min(MUSIC_TRACK_MIDI, settings.getInt("music_track", MUSIC_TRACK_SYNTH))
            );
            movementSpeed = Math.max(0, Math.min(3, settings.getInt("movement_speed", 0)));
            rayRacerMode = false;
            kuromiTheme = false;
            heartMode = false;
            settings.edit().remove("ray_racer_mode").remove("kuromi_theme").apply();
            sound.updateSound(soundEnabled, soundVolume);
        }

        private String adminModeNoteText() {
            if (adminMode) {
                return "管理员模式已开启：可以调整难度、白棋和炸药概率。";
            }
            return String.format(
                    Locale.US,
                    "普通模式：当前参数已锁定（难度 %.1f、白棋 %.1f、炸药 %.1f）。连续切换音乐 7 次可进入管理员模式。",
                    difficultyMultiplier,
                    whiteProbabilityMultiplier,
                    bombProbabilityMultiplier
            );
        }

        private void clearSpecialModes(boolean restoreConfiguredMusic) {
            rayRacerMode = false;
            kuromiTheme = false;
            heartMode = false;
            racerTrailCells.clear();
            racerTrailType = EMPTY;
            racerTrailUntil = 0L;
            if (restoreConfiguredMusic && musicTrack == MUSIC_TRACK_SY) {
                musicEnabled = settings.getBoolean("music_enabled", true);
                musicTrack = Math.max(
                        MUSIC_TRACK_SYNTH,
                        Math.min(MUSIC_TRACK_MIDI, settings.getInt("music_track", MUSIC_TRACK_SYNTH))
                );
                sound.updateMusic(musicEnabled, musicVolume, musicTrack);
            }
        }

        private void resetGame() {
            resetGame(false);
        }

        private void resetGame(boolean daily) {
            moveToken++;
            handler.removeCallbacksAndMessages(null);
            closeEasterDialog();
            dailyChallenge = daily;
            dailyRandomState = daily ? dailySeed() : 0L;
            dailyBestScore = daily
                    ? settings.getInt("daily_best_" + dateKey(), 0)
                    : 0;
            gameRecorded = false;
            tutorialStep = -1;
            tutorialSource = -1;
            tutorialTarget = -1;
            if (daily) {
                unlockAchievement("daily_player", "今日挑战");
            }
            adminMode = false;
            settingsMusicToggleCount = 0;
            adminUnlockedByMusic = false;
            settings.edit()
                    .putBoolean("admin_mode", false)
                    .apply();
            easterKind = EASTER_NONE;
            claimedEaster = EASTER_NONE;
            easterLoading = false;
            easterSecretTaps = 0;
            clearSpecialModes(true);
            sound.updateMusic(musicEnabled, musicVolume, activeMusicTrack());
            racerTrailCells.clear();
            racerTrailType = EMPTY;
            racerTrailUntil = 0L;
            for (int[] row : board) {
                Arrays.fill(row, EMPTY);
            }
            score = 0;
            removedCount = 0;
            moveCount = 0;
            lineClearCount = 0;
            bestClearCount = 0;
            currentChain = 0;
            bestChain = 0;
            bombsTriggered = 0;
            undoAvailable = false;
            undoBoard = null;
            undoPreview = null;
            selectedRow = -1;
            selectedCol = -1;
            spawnedThisTurn = false;
            clearedThisTurn = false;
            moving = false;
            spawning = false;
            removing = false;
            gameOver = false;
            dialogShowing = false;
            movePath.clear();
            clearSpawnedCells();
            clearPendingRemoval();
            preparePreview();
            spawnPieces();
            invalidate();
        }

        private void preparePreview() {
            for (int i = 0; i < PREVIEW_COUNT; i++) {
                preview[i] = randomPieceType();
            }
        }

        private int randomPieceType() {
            final double baseProbability = 1d / (NORMAL_COLORS + 2d);
            final double bombProbability = baseProbability
                    * (dailyChallenge ? DEFAULT_BOMB_PROBABILITY : bombProbabilityMultiplier);
            final double whiteProbability = baseProbability
                    * (dailyChallenge ? DEFAULT_WHITE_PROBABILITY : whiteProbabilityMultiplier);
            final double normalProbability = Math.max(
                    0d,
                    (1d - bombProbability - whiteProbability) / NORMAL_COLORS
            );
            double roll = nextRandomUnit();
            if (roll < bombProbability) {
                return BOMB;
            }
            roll -= bombProbability;
            if (roll < whiteProbability) {
                return WHITE;
            }
            roll -= whiteProbability;
            int color = (int) Math.floor(roll / Math.max(0.000001d, normalProbability)) + 1;
            return Math.max(1, Math.min(NORMAL_COLORS, color));
        }

        private double nextRandomUnit() {
            if (!dailyChallenge) {
                return random.nextDouble();
            }
            dailyRandomState = (dailyRandomState * 1664525L + 1013904223L) & 0xffffffffL;
            return dailyRandomState / 4294967296d;
        }

        private int nextRandomInt(int upperBound) {
            if (upperBound <= 1) {
                return 0;
            }
            return Math.min(upperBound - 1, (int) (nextRandomUnit() * upperBound));
        }

        private void shuffleCells(ArrayList<int[]> values) {
            if (!dailyChallenge) {
                Collections.shuffle(values, random);
                return;
            }
            for (int i = values.size() - 1; i > 0; i--) {
                int other = nextRandomInt(i + 1);
                int[] value = values.get(i);
                values.set(i, values.get(other));
                values.set(other, value);
            }
        }

        private int spawnCount() {
            int scoreExtra = 0;
            float activeDifficulty = dailyChallenge ? DEFAULT_DIFFICULTY : difficultyMultiplier;
            if (score >= 50 && activeDifficulty > 1f) {
                float factor = (score / 100f) * (activeDifficulty - 1f);
                scoreExtra = (int) Math.floor(BASE_SPAWN_COUNT * factor);
            }
            int progressExtra = Math.min(3, moveCount / 12);
            return Math.min(9, BASE_SPAWN_COUNT + scoreExtra + progressExtra);
        }

        private void spawnPieces() {
            spawnPieces(null);
        }

        private void spawnPieces(final Runnable afterAnimation) {
            ArrayList<int[]> emptyCells = new ArrayList<>();
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (board[row][col] == EMPTY) {
                        emptyCells.add(new int[]{row, col});
                    }
                }
            }
            shuffleCells(emptyCells);
            int count = Math.min(spawnCount(), emptyCells.size());
            clearSpawnedCells();
            for (int i = 0; i < count; i++) {
                int type = i < PREVIEW_COUNT ? preview[i] : randomPieceType();
                int[] cell = emptyCells.get(i);
                board[cell[0]][cell[1]] = type;
                spawnedCells[cell[0]][cell[1]] = true;
            }
            preparePreview();
            if (count > 0) {
                sound.playSpawn();
                spawning = true;
                spawnStart = SystemClock.uptimeMillis();
                final int token = moveToken;
                handler.postDelayed(() -> {
                    if (token != moveToken || !spawning) {
                        return;
                    }
                    spawning = false;
                    clearSpawnedCells();
                    if (tryStartCornerEasterEgg()) {
                        invalidate();
                        return;
                    }
                    if (afterAnimation != null) {
                        afterAnimation.run();
                    }
                    invalidate();
                }, SPAWN_DURATION_MS);
            } else {
                if (tryStartCornerEasterEgg()) {
                    invalidate();
                    return;
                }
                if (afterAnimation != null) {
                    afterAnimation.run();
                }
            }
        }

        private void clearSpawnedCells() {
            for (int row = 0; row < SIZE; row++) {
                Arrays.fill(spawnedCells[row], false);
            }
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            float usableWidth = width - BOARD_MARGIN * 2f;
            float contentHeight = height - statusInset - navigationInset;
            float usableHeight = contentHeight - TOP_HEIGHT - BOARD_MARGIN * 2f;
            cellSize = Math.min(usableWidth / SIZE, usableHeight / SIZE);
            boardSize = cellSize * SIZE;
            boardLeft = (width - boardSize) / 2f;
            boardTop = TOP_HEIGHT;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(0xff101820);
            canvas.save();
            canvas.translate(0f, statusInset);
            drawBackground(canvas);
            drawHeader(canvas);
            drawBoard(canvas);
            if (tutorialStep >= 0) {
                drawTutorial(canvas);
            }
            if (achievementToast != null && SystemClock.uptimeMillis() < achievementToastUntil) {
                drawAchievementToast(canvas);
            }
            if (gameOver) {
                drawGameOverOverlay(canvas);
            }
            canvas.restore();
            if (moving || spawning || removing || easterLoading
                    || SystemClock.uptimeMillis() < racerTrailUntil
                    || SystemClock.uptimeMillis() < achievementToastUntil) {
                postInvalidateOnAnimation();
            }
        }

        private void drawBackground(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setAlpha(255);
            paint.setColor(0xfff1eadf);
            canvas.drawRect(
                    0f,
                    0f,
                    getWidth(),
                    getHeight() - statusInset - navigationInset,
                    paint
            );
            paint.setColor(0xff72543e);
            canvas.drawRoundRect(
                    boardLeft - 7f,
                    boardTop - 7f,
                    boardLeft + boardSize + 7f,
                    boardTop + boardSize + 7f,
                    10f,
                    10f,
                    paint
            );
        }

        private void drawHeader(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setAlpha(255);
            paint.setColor(0xff101820);
            canvas.drawRect(0f, 0f, getWidth(), TOP_HEIGHT, paint);

            paint.setColor(Color.WHITE);
            paint.setFakeBoldText(true);
            paint.setTextSize(34f);
            canvas.drawText(dailyChallenge ? "\u6bcf\u65e5\u6311\u6218" : "\u4e94\u5b50\u6d88\u9664", 22f, 43f, paint);

            paint.setFakeBoldText(false);
            paint.setTextSize(14f);
            String subtitle = dailyChallenge
                    ? String.format(Locale.US, "\u4eca\u65e5\u6700\u4f73 %05d  \u00b7  \u6bcf12\u6b65\u63d0\u5347\u751f\u6210\u538b\u529b", Math.min(99999, dailyBestScore))
                    : "\u767d\u8272\u4e07\u80fd\u7403  \u00b7  \u70b8\u836f+\u81f3\u5c11\u56db\u9897\u540c\u8272\u7403\u6e05\u9664\u5168\u76d8";
            canvas.drawText(subtitle, 22f, 61f, paint);

            menuBounds.set(getWidth() - 174f, 24f, getWidth() - 18f, 78f);
            paint.setColor(0xffef5350);
            canvas.drawRoundRect(menuBounds, 15f, 15f, paint);
            paint.setColor(Color.WHITE);
            paint.setFakeBoldText(true);
            paint.setTextSize(19f);
            canvas.drawText("\u83dc\u5355", getWidth() - 132f, 58f, paint);
            paint.setFakeBoldText(false);

            drawScoreBar(canvas);
            drawPreview(canvas);
        }

        private void drawScoreBar(Canvas canvas) {
            float left = 20f;
            float top = 78f;
            float right = 300f;
            float bottom = TOP_HEIGHT - 12f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xff263746);
            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            paint.setColor(0xff607d8b);
            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xffc9d6df);
            paint.setFakeBoldText(true);
            paint.setTextSize(15f);
            canvas.drawText("\u5f97\u5206", left + 16f, top + 23f, paint);
            String scoreText = String.format(Locale.US, "%05d", Math.min(99999, score));
            paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            paint.setTextSize(29f);
            paint.setColor(Color.WHITE);
            canvas.drawText(scoreText, left + 78f, bottom - 15f, paint);
            paint.setTypeface(Typeface.DEFAULT);
            paint.setFakeBoldText(false);
        }

        private void drawPreview(Canvas canvas) {
            float left = Math.max(315f, getWidth() - 350f);
            float top = 78f;
            float right = getWidth() - 20f;
            float bottom = TOP_HEIGHT - 12f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xff263746);
            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            paint.setColor(0xff607d8b);
            canvas.drawRoundRect(left, top, right, bottom, 12f, 12f, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xffc9d6df);
            paint.setFakeBoldText(true);
            paint.setTextSize(15f);
            canvas.drawText("\u4e0b\u4e00\u8f6e", left + 14f, top + 23f, paint);
            paint.setFakeBoldText(false);
            float centerY = top + 47f;
            float firstX = left + 100f;
            float gap = 82f;
            for (int i = 0; i < PREVIEW_COUNT; i++) {
                if (preview[i] != EMPTY) {
                    drawPiece(canvas, preview[i], firstX + i * gap, centerY, 20f, false);
                }
            }
        }

        private void drawBoard(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setAlpha(255);
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    paint.setColor((row + col) % 2 == 0 ? 0xffdfc9aa : 0xffcfb38e);
                    canvas.drawRect(
                            boardLeft + col * cellSize,
                            boardTop + row * cellSize,
                            boardLeft + (col + 1) * cellSize,
                            boardTop + (row + 1) * cellSize,
                            paint
                    );
                }
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.6f);
            paint.setColor(0xff76583f);
            for (int i = 0; i <= SIZE; i++) {
                float x = boardLeft + i * cellSize;
                float y = boardTop + i * cellSize;
                canvas.drawLine(x, boardTop, x, boardTop + boardSize, paint);
                canvas.drawLine(boardLeft, y, boardLeft + boardSize, y, paint);
            }

            float removalProgress = removing ? removalProgress() : 0f;
            float spawnScale = spawning ? 0.05f + 0.95f * easeInOut(spawnProgress()) : 1f;
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    int type = board[row][col];
                    if (type == EMPTY) {
                        continue;
                    }
                    if (moving && isMovingTarget(row, col)) {
                        continue;
                    }
                    float radius = cellSize * 0.34f;
                    if (spawning && spawnedCells[row][col]) {
                        radius *= spawnScale;
                    }
                    if (removing && pendingRemove[row][col]) {
                        radius *= Math.max(0.02f, 1f - removalProgress);
                    }
                    drawPiece(canvas, type, centerX(col), centerY(row), radius, false);
                }
            }

            if (SystemClock.uptimeMillis() < racerTrailUntil && !racerTrailCells.isEmpty()) {
                drawRacerCellTrail(canvas);
            }
            if (moving && !movePath.isEmpty()) {
                drawMovingPiece(canvas);
            }
            if (removing) {
                drawExplosionEffects(canvas, removalProgress);
            }

            if (easterLoading) {
                drawEasterLoading(canvas);
            }

            if (selectedRow >= 0 && selectedCol >= 0 && !moving && !removing && !gameOver) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4.5f);
                paint.setColor(0xffdc2626);
                canvas.drawRect(
                        boardLeft + selectedCol * cellSize + 4f,
                        boardTop + selectedRow * cellSize + 4f,
                        boardLeft + (selectedCol + 1) * cellSize - 4f,
                        boardTop + (selectedRow + 1) * cellSize - 4f,
                        paint
                );
            }
        }

        private boolean isMovingTarget(int row, int col) {
            int[] last = movePath.get(movePath.size() - 1);
            return last[0] == row && last[1] == col;
        }

        private void drawMovingPiece(Canvas canvas) {
            int segments = Math.max(1, movePath.size() - 1);
            float progress = Math.max(
                    0f,
                    Math.min(1f, (SystemClock.uptimeMillis() - moveStart) / (float) moveDuration)
            );
            float scaled = progress * segments;
            int segment = Math.min(segments - 1, (int) Math.floor(scaled));
            float local = Math.min(1f, scaled - segment);
            int[] from = movePath.get(segment);
            int[] to = movePath.get(segment + 1);
            float x = lerp(centerX(from[1]), centerX(to[1]), easeInOut(local));
            float y = lerp(centerY(from[0]), centerY(to[0]), easeInOut(local));
            if (rayRacerMode) {
                drawRacerTrail(canvas, scaled);
            }
            if (heartMode && movingType != BOMB) {
                drawHeartMoveTrail(canvas, scaled);
            }
            drawPiece(canvas, movingType, x, y, cellSize * 0.34f, true);
        }

        private void drawHeartMoveTrail(Canvas canvas, float scaledProgress) {
            for (int i = 8; i >= 1; i--) {
                float trailProgress = Math.max(0f, scaledProgress - i * 0.18f);
                float[] point = pointOnMovePath(trailProgress);
                int alpha = Math.max(26, 172 - i * 18);
                float radius = cellSize * (0.24f - i * 0.010f);
                drawHeartShape(canvas, point[0], point[1], radius, colorFor(movingType), alpha, false);
            }
        }

        private void drawRacerTrail(Canvas canvas, float scaledProgress) {
            for (int i = 7; i >= 1; i--) {
                float trailProgress = Math.max(0f, scaledProgress - i * 0.22f);
                float[] point = pointOnMovePath(trailProgress);
                int alpha = Math.max(24, 150 - i * 17);
                float radius = cellSize * (0.33f - i * 0.018f);
                drawTrailDot(canvas, movingType, point[0], point[1], radius, alpha);
            }
        }

        private float[] pointOnMovePath(float scaledProgress) {
            int segments = Math.max(1, movePath.size() - 1);
            int segment = Math.min(segments - 1, (int) Math.floor(scaledProgress));
            float local = Math.min(1f, scaledProgress - segment);
            int[] from = movePath.get(segment);
            int[] to = movePath.get(segment + 1);
            return new float[]{
                    lerp(centerX(from[1]), centerX(to[1]), easeInOut(local)),
                    lerp(centerY(from[0]), centerY(to[0]), easeInOut(local))
            };
        }

        private void drawTrailDot(Canvas canvas, int type, float cx, float cy, float radius, int alpha) {
            if (alpha <= 1) {
                return;
            }
            int baseColor = colorFor(type);
            paint.setShader(new RadialGradient(
                    cx,
                    cy,
                    radius * 1.6f,
                    new int[]{lighten(baseColor, 0.25f), baseColor, darken(baseColor, 0.45f)},
                    new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP
            ));
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(alpha);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);
            paint.setAlpha(Math.min(120, alpha + 28));
            paint.setColor(lighten(baseColor, 0.35f));
            canvas.drawCircle(cx - radius * 0.26f, cy - radius * 0.28f, radius * 0.16f, paint);
            paint.setAlpha(255);
        }

        private void drawRacerCellTrail(Canvas canvas) {
            long now = SystemClock.uptimeMillis();
            float life = Math.max(0f, Math.min(1f, (racerTrailUntil - now) / 260f));
            int count = racerTrailCells.size();
            for (int i = 0; i < count; i++) {
                int[] cell = racerTrailCells.get(i);
                float rank = count <= 1 ? 1f : i / (float) (count - 1);
                int alpha = Math.round((35f + rank * 150f) * life);
                float radius = cellSize * (0.20f + rank * 0.12f);
                drawTrailDot(canvas, racerTrailType, centerX(cell[1]), centerY(cell[0]), radius, alpha);
            }
        }

        private void drawExplosionEffects(Canvas canvas, float progress) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            for (int[] bomb : pendingBombs) {
                float cx = centerX(bomb[1]);
                float cy = centerY(bomb[0]);
                paint.setAlpha(Math.round(220f * (1f - progress)));
                paint.setStrokeWidth(Math.max(3f, cellSize * 0.055f * (1f - progress * 0.35f)));
                paint.setColor(0xffff6d00);
                canvas.drawCircle(cx, cy, cellSize * (0.22f + 0.82f * progress), paint);
                paint.setAlpha(Math.round(150f * (1f - progress)));
                paint.setColor(0xffffc107);
                canvas.drawCircle(cx, cy, cellSize * (0.10f + 0.45f * progress), paint);
                if (heartMode) {
                    drawExplosionHearts(canvas, cx, cy, progress);
                }
            }
            paint.setAlpha(255);
        }

        private void drawExplosionHearts(Canvas canvas, float cx, float cy, float progress) {
            int[] colors = {
                    0xffe10600,
                    0xffff4fb0,
                    0xffffd000,
                    0xff00b4d8,
                    0xff7209b7,
                    0xff00a86b,
                    0xffff7a00,
                    0xffff6aa2
            };
            float distance = cellSize * (0.22f + progress * 1.05f);
            int alpha = Math.round(230f * Math.max(0f, 1f - progress));
            for (int i = 0; i < colors.length; i++) {
                double angle = -Math.PI / 2d + i * (Math.PI * 2d / colors.length);
                float x = cx + (float) Math.cos(angle) * distance;
                float y = cy + (float) Math.sin(angle) * distance;
                float radius = cellSize * (0.12f + 0.04f * (1f - progress));
                drawHeartShape(canvas, x, y, radius, colors[i], alpha, false);
            }
        }

        private float spawnProgress() {
            return Math.max(
                    0f,
                    Math.min(
                            1f,
                            (SystemClock.uptimeMillis() - spawnStart)
                                    / (float) SPAWN_DURATION_MS
                    )
            );
        }

        private void drawPiece(Canvas canvas, int type, float cx, float cy, float radius, boolean movingPiece) {
            if (radius <= 0.5f) {
                return;
            }
            if (type == BOMB) {
                drawBomb(canvas, cx, cy, radius, movingPiece);
                return;
            }
            if (heartMode) {
                drawHeartPiece(canvas, type, cx, cy, radius, movingPiece);
                return;
            }
            if (rayRacerMode) {
                drawCarLogoPiece(canvas, type, cx, cy, radius, movingPiece);
                return;
            }
            if (kuromiTheme) {
                drawKuromiPiece(canvas, type, cx, cy, radius, movingPiece);
                return;
            }
            float shadowRadius = radius * 1.03f;
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setAlpha(0x3a);
            paint.setColor(0x3a2b2119);
            canvas.drawCircle(cx + radius * 0.08f, cy + radius * 0.11f, shadowRadius, paint);

            int baseColor = colorFor(type);
            paint.setAlpha(255);
            paint.setShader(new RadialGradient(
                    cx - radius * 0.34f,
                    cy - radius * 0.40f,
                    radius * 1.35f,
                    new int[]{
                            lighten(baseColor, 0.18f),
                            lighten(baseColor, 0.02f),
                            baseColor,
                            darken(baseColor, 0.52f)
                    },
                    new float[]{0f, 0.24f, 0.62f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(movingPiece ? 4.5f : 2.2f);
            paint.setColor(type == WHITE ? 0xff7b8794 : darken(baseColor, 0.62f));
            canvas.drawCircle(cx, cy, radius, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(0xb8);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx - radius * 0.30f, cy - radius * 0.34f, radius * 0.16f, paint);
            paint.setAlpha(0x42);
            shineBounds.set(
                    cx - radius * 0.56f,
                    cy - radius * 0.10f,
                    cx - radius * 0.20f,
                    cy + radius * 0.05f
            );
            canvas.drawOval(shineBounds, paint);
            paint.setAlpha(255);
        }

        private void drawHeartPiece(Canvas canvas, int type, float cx, float cy, float radius, boolean movingPiece) {
            int baseColor = colorFor(type);
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(0x44);
            paint.setColor(0x442b2119);
            drawHeartShape(canvas, cx + radius * 0.10f, cy + radius * 0.13f, radius * 1.02f, 0xff2b2119, 64, false);

            paint.setShader(new RadialGradient(
                    cx - radius * 0.30f,
                    cy - radius * 0.40f,
                    radius * 1.35f,
                    new int[]{
                            lighten(baseColor, 0.24f),
                            baseColor,
                            darken(baseColor, 0.50f)
                    },
                    new float[]{0f, 0.58f, 1f},
                    Shader.TileMode.CLAMP
            ));
            drawHeartShape(canvas, cx, cy, radius, baseColor, 255, false);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(movingPiece ? 4.2f : 2.3f);
            paint.setColor(type == WHITE ? 0xff8b949e : darken(baseColor, 0.56f));
            drawHeartShape(canvas, cx, cy, radius, baseColor, 255, true);

            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(0xc8);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx - radius * 0.25f, cy - radius * 0.32f, radius * 0.13f, paint);
            paint.setAlpha(0x58);
            shineBounds.set(
                    cx - radius * 0.50f,
                    cy - radius * 0.07f,
                    cx - radius * 0.20f,
                    cy + radius * 0.10f
            );
            canvas.drawOval(shineBounds, paint);
            paint.setAlpha(255);
        }

        private void drawHeartShape(Canvas canvas, float cx, float cy, float radius, int color, int alpha, boolean stroke) {
            Path heart = new Path();
            heart.moveTo(cx, cy + radius * 0.62f);
            heart.cubicTo(
                    cx - radius * 1.10f,
                    cy - radius * 0.03f,
                    cx - radius * 0.78f,
                    cy - radius * 0.86f,
                    cx - radius * 0.22f,
                    cy - radius * 0.62f
            );
            heart.cubicTo(
                    cx - radius * 0.06f,
                    cy - radius * 0.55f,
                    cx,
                    cy - radius * 0.40f,
                    cx,
                    cy - radius * 0.28f
            );
            heart.cubicTo(
                    cx,
                    cy - radius * 0.40f,
                    cx + radius * 0.06f,
                    cy - radius * 0.55f,
                    cx + radius * 0.22f,
                    cy - radius * 0.62f
            );
            heart.cubicTo(
                    cx + radius * 0.78f,
                    cy - radius * 0.86f,
                    cx + radius * 1.10f,
                    cy - radius * 0.03f,
                    cx,
                    cy + radius * 0.62f
            );
            heart.close();
            paint.setAlpha(alpha);
            if (stroke) {
                paint.setStyle(Paint.Style.STROKE);
            } else {
                paint.setStyle(Paint.Style.FILL);
                if (paint.getShader() == null) {
                    paint.setColor(color);
                }
            }
            canvas.drawPath(heart, paint);
            paint.setAlpha(255);
        }

        private void drawBomb(Canvas canvas, float cx, float cy, float radius, boolean movingPiece) {
            canvas.save();
            canvas.rotate(-16f, cx, cy);
            float bodyLeft = cx - radius * 0.36f;
            float bodyRight = cx + radius * 0.36f;
            float bodyTop = cy - radius * 0.98f;
            float bodyBottom = cy + radius * 0.98f;
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(0x4d);
            paint.setColor(0x4d2b2119);
            canvas.drawRoundRect(
                    bodyLeft + radius * 0.10f,
                    bodyTop + radius * 0.12f,
                    bodyRight + radius * 0.10f,
                    bodyBottom + radius * 0.12f,
                    radius * 0.15f,
                    radius * 0.15f,
                    paint
            );
            paint.setAlpha(255);
            paint.setColor(0xffb71c1c);
            canvas.drawRoundRect(bodyLeft, bodyTop, bodyRight, bodyBottom, radius * 0.15f, radius * 0.15f, paint);
            paint.setColor(0xffef5350);
            canvas.drawRoundRect(
                    bodyLeft + radius * 0.09f,
                    bodyTop + radius * 0.08f,
                    bodyLeft + radius * 0.20f,
                    bodyBottom - radius * 0.12f,
                    radius * 0.06f,
                    radius * 0.06f,
                    paint
            );
            paint.setColor(0xff6d1515);
            canvas.drawRect(bodyLeft, cy - radius * 0.53f, bodyRight, cy - radius * 0.40f, paint);
            canvas.drawRect(bodyLeft, cy + radius * 0.38f, bodyRight, cy + radius * 0.51f, paint);
            paint.setColor(0xff4e342e);
            canvas.drawRect(
                    cx - radius * 0.11f,
                    bodyTop - radius * 0.18f,
                    cx + radius * 0.11f,
                    bodyTop + radius * 0.05f,
                    paint
            );
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(Math.max(2f, radius * 0.075f));
            paint.setColor(0xff4e342e);
            Path fuse = new Path();
            fuse.moveTo(cx, bodyTop - radius * 0.18f);
            fuse.cubicTo(
                    cx + radius * 0.34f,
                    bodyTop - radius * 0.46f,
                    cx + radius * 0.45f,
                    bodyTop - radius * 0.30f,
                    cx + radius * 0.56f,
                    bodyTop - radius * 0.54f
            );
            canvas.drawPath(fuse, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xffffc107);
            canvas.drawCircle(cx + radius * 0.57f, bodyTop - radius * 0.56f, radius * 0.13f, paint);
            paint.setColor(0xfffff59d);
            canvas.drawCircle(cx + radius * 0.53f, bodyTop - radius * 0.60f, radius * 0.05f, paint);
            paint.setAlpha(255);
            canvas.restore();
        }

        private void drawKuromiPiece(Canvas canvas, int type, float cx, float cy, float radius, boolean movingPiece) {
            int outerColor = themedOuterColor(type);
            int outlineColor = type == WHITE ? 0xffd9c8a8 : darken(outerColor, 0.54f);
            canvas.save();
            paint.setShader(null);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(false);

            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(0x42);
            paint.setColor(0xff2b2119);
            canvas.drawCircle(cx + radius * 0.09f, cy + radius * 0.14f, radius * 1.04f, paint);

            drawKuromiEar(canvas, cx, cy, radius, -1f, outerColor);
            drawKuromiEar(canvas, cx, cy, radius, 1f, outerColor);

            paint.setAlpha(255);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(
                    cx - radius * 0.36f,
                    cy - radius * 0.46f,
                    radius * 1.55f,
                    new int[]{lighten(outerColor, 0.22f), outerColor, darken(outerColor, 0.44f)},
                    new float[]{0f, 0.56f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(movingPiece ? 4.1f : 2.2f);
            paint.setColor(outlineColor);
            canvas.drawCircle(cx, cy, radius, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xfffbfaf4);
            Path face = new Path();
            face.moveTo(cx - radius * 0.72f, cy - radius * 0.03f);
            face.cubicTo(cx - radius * 0.68f, cy - radius * 0.44f, cx - radius * 0.31f, cy - radius * 0.42f, cx - radius * 0.04f, cy - radius * 0.15f);
            face.quadTo(cx, cy - radius * 0.09f, cx + radius * 0.04f, cy - radius * 0.15f);
            face.cubicTo(cx + radius * 0.31f, cy - radius * 0.42f, cx + radius * 0.68f, cy - radius * 0.44f, cx + radius * 0.72f, cy - radius * 0.03f);
            face.cubicTo(cx + radius * 0.82f, cy + radius * 0.52f, cx + radius * 0.39f, cy + radius * 0.78f, cx, cy + radius * 0.77f);
            face.cubicTo(cx - radius * 0.39f, cy + radius * 0.78f, cx - radius * 0.82f, cy + radius * 0.52f, cx - radius * 0.72f, cy - radius * 0.03f);
            face.close();
            canvas.drawPath(face, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xff111118);
            shineBounds.set(cx - radius * 0.45f, cy - radius * 0.02f, cx - radius * 0.19f, cy + radius * 0.45f);
            canvas.drawOval(shineBounds, paint);
            shineBounds.set(cx + radius * 0.19f, cy - radius * 0.02f, cx + radius * 0.45f, cy + radius * 0.45f);
            canvas.drawOval(shineBounds, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.5f, radius * 0.07f));
            paint.setColor(0xff111118);
            canvas.drawLine(cx - radius * 0.43f, cy + radius * 0.02f, cx - radius * 0.58f, cy - radius * 0.08f, paint);
            canvas.drawLine(cx - radius * 0.42f, cy + radius * 0.12f, cx - radius * 0.58f, cy + radius * 0.07f, paint);
            canvas.drawLine(cx + radius * 0.43f, cy + radius * 0.02f, cx + radius * 0.58f, cy - radius * 0.08f, paint);
            canvas.drawLine(cx + radius * 0.42f, cy + radius * 0.12f, cx + radius * 0.58f, cy + radius * 0.07f, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xffff9eb7);
            shineBounds.set(cx - radius * 0.67f, cy + radius * 0.35f, cx - radius * 0.36f, cy + radius * 0.55f);
            canvas.drawOval(shineBounds, paint);
            shineBounds.set(cx + radius * 0.36f, cy + radius * 0.35f, cx + radius * 0.67f, cy + radius * 0.55f);
            canvas.drawOval(shineBounds, paint);
            paint.setColor(0xffff6f9f);
            shineBounds.set(cx - radius * 0.08f, cy + radius * 0.24f, cx + radius * 0.08f, cy + radius * 0.34f);
            canvas.drawOval(shineBounds, paint);

            drawKuromiSkull(canvas, cx, cy - radius * 0.45f, radius * 0.28f);

            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(0xb8);
            paint.setColor(Color.WHITE);
            shineBounds.set(cx - radius * 0.64f, cy - radius * 0.61f, cx - radius * 0.22f, cy - radius * 0.40f);
            canvas.drawOval(shineBounds, paint);
            paint.setAlpha(0x8f);
            canvas.drawCircle(cx - radius * 0.10f, cy - radius * 0.66f, radius * 0.09f, paint);
            paint.setAlpha(255);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.restore();
        }

        private void drawKuromiEar(Canvas canvas, float cx, float cy, float radius, float side, int outerColor) {
            Path ear = new Path();
            ear.moveTo(cx + side * radius * 0.35f, cy - radius * 0.72f);
            ear.cubicTo(
                    cx + side * radius * 0.49f,
                    cy - radius * 1.12f,
                    cx + side * radius * 0.69f,
                    cy - radius * 1.41f,
                    cx + side * radius * 0.94f,
                    cy - radius * 1.43f
            );
            ear.cubicTo(
                    cx + side * radius * 1.13f,
                    cy - radius * 1.45f,
                    cx + side * radius * 1.16f,
                    cy - radius * 1.09f,
                    cx + side * radius * 1.22f,
                    cy - radius * 0.75f
            );
            ear.quadTo(cx + side * radius * 1.22f, cy - radius * 0.54f,
                    cx + side * radius * 0.81f, cy - radius * 0.59f);
            ear.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(255);
            paint.setShader(new RadialGradient(
                    cx + side * radius * 0.76f,
                    cy - radius * 1.20f,
                    radius * 0.74f,
                    new int[]{lighten(outerColor, 0.25f), outerColor, darken(outerColor, 0.45f)},
                    new float[]{0f, 0.62f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawPath(ear, paint);
            paint.setShader(null);
            paint.setColor(darken(outerColor, 0.35f));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.2f, radius * 0.035f));
            canvas.drawPath(ear, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(
                    cx + side * radius * 1.02f,
                    cy - radius * 1.44f,
                    radius * 0.32f,
                    new int[]{lighten(outerColor, 0.25f), outerColor, darken(outerColor, 0.45f)},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawCircle(cx + side * radius * 1.02f, cy - radius * 1.44f, radius * 0.20f, paint);
            paint.setShader(null);
            paint.setAlpha(0x9a);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx + side * radius * 0.96f, cy - radius * 1.52f, radius * 0.07f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.1f, radius * 0.055f));
            canvas.drawLine(cx + side * radius * 0.65f, cy - radius * 1.20f,
                    cx + side * radius * 0.86f, cy - radius * 1.33f, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(255);
        }

        private void drawKuromiSkull(Canvas canvas, float cx, float cy, float radius) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xffff9ab3);
            shineBounds.set(cx - radius * 0.78f, cy - radius * 0.62f, cx + radius * 0.78f, cy + radius * 0.58f);
            canvas.drawOval(shineBounds, paint);
            float toothTop = cy + radius * 0.30f;
            float toothBottom = cy + radius * 0.92f;
            for (int i = -1; i <= 1; i++) {
                shineBounds.set(
                        cx + i * radius * 0.38f - radius * 0.16f,
                        toothTop,
                        cx + i * radius * 0.38f + radius * 0.16f,
                        toothBottom
                );
                canvas.drawRoundRect(shineBounds, radius * 0.10f, radius * 0.10f, paint);
            }
            paint.setColor(0xff111118);
            canvas.drawCircle(cx - radius * 0.28f, cy - radius * 0.08f, radius * 0.17f, paint);
            canvas.drawCircle(cx + radius * 0.28f, cy - radius * 0.08f, radius * 0.17f, paint);
        }

        private void drawCarLogoPiece(Canvas canvas, int type, float cx, float cy, float radius, boolean movingPiece) {
            int outerColor = carOuterColorFor(type);
            canvas.save();
            paint.setShader(null);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setFakeBoldText(false);

            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(0x46);
            paint.setColor(0xff2b2119);
            canvas.drawCircle(cx + radius * 0.10f, cy + radius * 0.13f, radius * 1.04f, paint);

            paint.setAlpha(255);
            paint.setShader(new RadialGradient(
                    cx - radius * 0.34f,
                    cy - radius * 0.42f,
                    radius * 1.55f,
                    new int[]{lighten(outerColor, 0.25f), outerColor, darken(outerColor, 0.48f)},
                    new float[]{0f, 0.58f, 1f},
                    Shader.TileMode.CLAMP
            ));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(movingPiece ? 4.3f : 2.5f);
            paint.setColor(type == WHITE ? 0xffd8c8aa : darken(outerColor, 0.55f));
            canvas.drawCircle(cx, cy, radius, paint);

            int plateColor = type == WHITE ? 0xff2c3036 : 0xff20242b;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(plateColor);
            canvas.drawCircle(cx, cy, radius * 0.81f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.3f, radius * 0.045f));
            paint.setColor(0xffd8dde6);
            canvas.drawCircle(cx, cy, radius * 0.82f, paint);
            paint.setColor(0xff6f7785);
            paint.setStrokeWidth(Math.max(1.0f, radius * 0.022f));
            canvas.drawCircle(cx, cy, radius * 0.73f, paint);

            Bitmap logo = carLogoFor(carBrandFor(type));
            if (logo != null) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.WHITE);
                paint.setAlpha(255);
                paint.setFilterBitmap(true);
                shineBounds.set(cx - radius * 0.82f, cy - radius * 0.82f,
                        cx + radius * 0.82f, cy + radius * 0.82f);
                canvas.drawBitmap(logo, null, shineBounds, paint);
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(0xb0);
            paint.setColor(Color.WHITE);
            shineBounds.set(cx - radius * 0.62f, cy - radius * 0.66f, cx - radius * 0.22f, cy - radius * 0.46f);
            canvas.drawOval(shineBounds, paint);
            paint.setAlpha(255);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setFakeBoldText(false);
            canvas.restore();
        }

        private int carBrandFor(int type) {
            switch (type) {
                case 1:
                    return 1;
                case 2:
                    return 2;
                case 3:
                    return 3;
                case 4:
                    return 4;
                case 5:
                    return 5;
                case 6:
                    return 6;
                case 7:
                    return 7;
                case 8:
                    return 8;
                case WHITE:
                default:
                    return 9;
            }
        }

        private Bitmap carLogoFor(int brand) {
            if (brand < 1 || brand >= carLogoBitmaps.length) return null;
            if (carLogoBitmaps[brand] == null) {
                int[] resources = {0, R.drawable.car_ferrari, R.drawable.car_lamborghini,
                        R.drawable.car_porsche, R.drawable.car_dodge, R.drawable.car_cadillac,
                        R.drawable.car_nissan, R.drawable.car_mercedes, R.drawable.car_bmw,
                        R.drawable.car_audi};
                carLogoBitmaps[brand] = BitmapFactory.decodeResource(getResources(), resources[brand]);
            }
            return carLogoBitmaps[brand];
        }

        private int themedOuterColor(int type) {
            return type == WHITE ? 0xfff1e3c5 : colorFor(type);
        }

        private int carOuterColorFor(int type) {
            return type == WHITE ? 0xfff1e3c5 : colorFor(type);
        }

        private int lighten(int color, float amount) {
            int r = Color.red(color);
            int g = Color.green(color);
            int b = Color.blue(color);
            return Color.rgb(
                    r + Math.round((255 - r) * amount),
                    g + Math.round((255 - g) * amount),
                    b + Math.round((255 - b) * amount)
            );
        }

        private int darken(int color, float amount) {
            return Color.rgb(
                    Math.round(Color.red(color) * (1f - amount)),
                    Math.round(Color.green(color) * (1f - amount)),
                    Math.round(Color.blue(color) * (1f - amount))
            );
        }

        private int colorFor(int type) {
            switch (type) {
                case 1:
                    return 0xffe10600;
                case 2:
                    return 0xffffd000;
                case 3:
                    return 0xff00a86b;
                case 4:
                    return 0xff7209b7;
                case 5:
                    return 0xffff00a8;
                case 6:
                    return 0xff00b4d8;
                case 7:
                    return 0xff121212;
                case 8:
                    return 0xff0057d9;
                case WHITE:
                    return 0xffffffff;
                default:
                    return 0xff9ca3af;
            }
        }

        private void drawGameOverOverlay(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(0xaa);
            paint.setColor(Color.BLACK);
            float contentHeight = getHeight() - statusInset - navigationInset;
            canvas.drawRect(0f, 0f, getWidth(), contentHeight, paint);
            paint.setAlpha(255);
            paint.setColor(Color.WHITE);
            paint.setFakeBoldText(true);
            paint.setTextSize(38f);
            canvas.drawText("\u6e38\u620f\u7ed3\u675f", getWidth() / 2f - 76f, contentHeight / 2f - 18f, paint);
            paint.setFakeBoldText(false);
            paint.setTextSize(25f);
            canvas.drawText("\u6700\u7ec8\u5f97\u5206  " + score, getWidth() / 2f - 76f, contentHeight / 2f + 28f, paint);
        }

        private void drawTutorial(Canvas canvas) {
            RectF focus = new RectF();
            String message;
            boolean action = false;
            if (tutorialStep == 0 || tutorialStep == 1) {
                int index = tutorialStep == 0 ? tutorialSource : tutorialTarget;
                if (index < 0) return;
                int row = index / SIZE;
                int col = index % SIZE;
                focus.set(
                        boardLeft + col * cellSize + 3f,
                        boardTop + row * cellSize + 3f,
                        boardLeft + (col + 1) * cellSize - 3f,
                        boardTop + (row + 1) * cellSize - 3f
                );
                message = tutorialStep == 0
                        ? "\u7b2c1\u6b65\uff1a\u70b9\u51fb\u9ad8\u4eae\u68cb\u5b50"
                        : "\u7b2c2\u6b65\uff1a\u70b9\u51fb\u9ad8\u4eae\u7a7a\u683c";
            } else if (tutorialStep == 2) {
                float left = Math.max(315f, getWidth() - 350f);
                focus.set(left, 78f, getWidth() - 20f, TOP_HEIGHT - 12f);
                message = "\u7b2c3\u6b65\uff1a\u89c2\u5bdf\u4e0b\u4e00\u8f6e\u68cb\u5b50";
                action = true;
            } else {
                focus.set(menuBounds);
                message = "\u7b2c4\u6b65\uff1a\u70b9\u51fb\u83dc\u5355\u67e5\u770b\u66f4\u591a\u73a9\u6cd5";
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xa6000000);
            float contentBottom = getHeight() - statusInset - navigationInset;
            canvas.drawRect(0f, 0f, getWidth(), focus.top, paint);
            canvas.drawRect(0f, focus.bottom, getWidth(), contentBottom, paint);
            canvas.drawRect(0f, focus.top, focus.left, focus.bottom, paint);
            canvas.drawRect(focus.right, focus.top, getWidth(), focus.bottom, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(0xffffd000);
            canvas.drawRoundRect(focus, 9f, 9f, paint);

            float boxLeft = 24f;
            float boxRight = getWidth() - 24f;
            float boxHeight = action ? 104f : 72f;
            float boxTop = focus.centerY() < boardTop + boardSize / 2f
                    ? boardTop + boardSize - boxHeight - 18f
                    : boardTop + 18f;
            float boxBottom = boxTop + boxHeight;
            tutorialActionBounds.setEmpty();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xff101820);
            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 12f, 12f, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(17f);
            paint.setFakeBoldText(true);
            canvas.drawText(message, boxLeft + 16f, boxTop + 30f, paint);
            if (action) {
                tutorialActionBounds.set(boxRight - 112f, boxTop + 48f, boxRight - 12f, boxBottom - 10f);
                paint.setColor(0xffef5350);
                canvas.drawRoundRect(tutorialActionBounds, 9f, 9f, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(16f);
                canvas.drawText("\u4e0b\u4e00\u6b65", tutorialActionBounds.left + 20f, tutorialActionBounds.centerY() + 6f, paint);
            }
            paint.setFakeBoldText(false);
        }

        private void drawAchievementToast(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xff101820);
            float left = 28f;
            float right = getWidth() - 28f;
            float top = TOP_HEIGHT + 12f;
            float bottom = top + 48f;
            canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(17f);
            paint.setFakeBoldText(true);
            canvas.drawText(achievementToast, left + 18f, top + 31f, paint);
            paint.setFakeBoldText(false);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) {
                return true;
            }
            float x = event.getX();
            float y = event.getY() - statusInset;
            if (tutorialStep == 2) {
                if (tutorialActionBounds.contains(x, y)) {
                    tutorialStep = 3;
                    sound.playClick();
                    invalidate();
                }
                return true;
            }
            if (menuBounds.contains(x, y)) {
                if (tutorialStep == 3) {
                    finishTutorial();
                } else if (tutorialStep >= 0) {
                    return true;
                }
                showMenu();
                return true;
            }
            if (gameOver || moving || spawning || removing) {
                return true;
            }
            int[] cell = hitCell(x, y);
            if (cell == null) {
                return true;
            }
            int row = cell[0];
            int col = cell[1];
            if (tutorialStep == 0) {
                if (row * SIZE + col == tutorialSource) {
                    selectedRow = row;
                    selectedCol = col;
                    tutorialStep = 1;
                    sound.playClick();
                    invalidate();
                }
                return true;
            }
            if (tutorialStep == 1) {
                if (row * SIZE + col != tutorialTarget) {
                    return true;
                }
                ArrayList<int[]> path = findShortestPath(selectedRow, selectedCol, row, col);
                if (path != null) {
                    captureUndoState();
                    startMove(path);
                }
                return true;
            }
            if (board[row][col] != EMPTY) {
                selectedRow = row;
                selectedCol = col;
                sound.playClick();
                invalidate();
                return true;
            }
            if (selectedRow >= 0 && selectedCol >= 0) {
                ArrayList<int[]> path = findShortestPath(selectedRow, selectedCol, row, col);
                if (path != null) {
                    captureUndoState();
                    startMove(path);
                }
            }
            return true;
        }

        private int[] hitCell(float x, float y) {
            int col = (int) ((x - boardLeft) / cellSize);
            int row = (int) ((y - boardTop) / cellSize);
            if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
                return null;
            }
            return new int[]{row, col};
        }

        private ArrayList<int[]> findShortestPath(int fromRow, int fromCol, int toRow, int toCol) {
            if (!inside(fromRow, fromCol)
                    || !inside(toRow, toCol)
                    || board[fromRow][fromCol] == EMPTY
                    || board[toRow][toCol] != EMPTY) {
                return null;
            }
            boolean[][] visited = new boolean[SIZE][SIZE];
            int[][] previousRow = new int[SIZE][SIZE];
            int[][] previousCol = new int[SIZE][SIZE];
            for (int row = 0; row < SIZE; row++) {
                Arrays.fill(previousRow[row], -1);
                Arrays.fill(previousCol[row], -1);
            }
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            queue.add(new int[]{fromRow, fromCol});
            visited[fromRow][fromCol] = true;
            int[] rowStep = {1, -1, 0, 0};
            int[] colStep = {0, 0, 1, -1};
            while (!queue.isEmpty()) {
                int[] current = queue.removeFirst();
                if (current[0] == toRow && current[1] == toCol) {
                    break;
                }
                for (int i = 0; i < 4; i++) {
                    int nextRow = current[0] + rowStep[i];
                    int nextCol = current[1] + colStep[i];
                    if (!inside(nextRow, nextCol) || visited[nextRow][nextCol]) {
                        continue;
                    }
                    if (board[nextRow][nextCol] != EMPTY
                            && !(nextRow == toRow && nextCol == toCol)) {
                        continue;
                    }
                    visited[nextRow][nextCol] = true;
                    previousRow[nextRow][nextCol] = current[0];
                    previousCol[nextRow][nextCol] = current[1];
                    queue.addLast(new int[]{nextRow, nextCol});
                }
            }
            if (!visited[toRow][toCol]) {
                return null;
            }
            ArrayList<int[]> path = new ArrayList<>();
            int row = toRow;
            int col = toCol;
            while (row >= 0 && col >= 0) {
                path.add(new int[]{row, col});
                if (row == fromRow && col == fromCol) {
                    break;
                }
                int previousR = previousRow[row][col];
                int previousC = previousCol[row][col];
                row = previousR;
                col = previousC;
            }
            if (path.get(path.size() - 1)[0] != fromRow
                    || path.get(path.size() - 1)[1] != fromCol) {
                return null;
            }
            Collections.reverse(path);
            return path;
        }

        private void startMove(ArrayList<int[]> path) {
            final int token = ++moveToken;
            int[] from = path.get(0);
            int[] to = path.get(path.size() - 1);
            movingType = board[from[0]][from[1]];
            moveCount++;
            currentChain = 0;
            if (moveCount >= 30) unlockAchievement("patient_30", "\u6df1\u8c0b\u8fdc\u8651");
            board[from[0]][from[1]] = EMPTY;
            selectedRow = -1;
            selectedCol = -1;
            movePath = path;
            moveStart = SystemClock.uptimeMillis();
            moveDuration = movementStepDuration() * Math.max(1, path.size() - 1);
            if (rayRacerMode) {
                racerTrailCells.clear();
                for (int[] cell : path) {
                    racerTrailCells.add(new int[]{cell[0], cell[1]});
                }
                racerTrailType = movingType;
                racerTrailUntil = moveStart + moveDuration + 260L;
            }
            moving = true;
            sound.playMove();
            invalidate();
            handler.postDelayed(() -> {
                if (token != moveToken || !moving) {
                    return;
                }
                board[to[0]][to[1]] = movingType;
                moving = false;
                movePath.clear();
                if (tutorialStep == 1) {
                    tutorialStep = 2;
                }
        spawnedThisTurn = false;
        clearedThisTurn = false;
        clearedThisTurn = false;
                if (tryStartCornerEasterEgg()) {
                    invalidate();
                    return;
                }
        beginResolution(token);
                invalidate();
            }, moveDuration + 20L);
        }

        private void beginResolution(final int token) {
            if (token != moveToken || moving || removing || gameOver) {
                return;
            }
            if (prepareRemovalPlan()) {
                startRemovalAnimation(token);
                return;
            }
        if (isFull()) {
                finishGame();
                return;
            }
        if (clearedThisTurn || spawnedThisTurn) {
                return;
            }
        spawnedThisTurn = true;
            spawnPieces(() -> {
                if (token != moveToken || gameOver) {
                    return;
                }
                if (prepareRemovalPlan()) {
                    startRemovalAnimation(token);
                } else if (isFull()) {
                    finishGame();
                }
            });
        }

        private long movementStepDuration() {
            if (rayRacerMode) {
                return 4L;
            }
            switch (movementSpeed) {
                case 1:
                    return 135L;
                case 2:
                    return 80L;
                case 3:
                    return 35L;
                default:
                    return 220L;
            }
        }

        private void startRemovalAnimation(final int token) {
            removing = true;
            removalStart = SystemClock.uptimeMillis();
            currentChain++;
            bestChain = Math.max(bestChain, currentChain);
            if (currentChain >= 2) unlockAchievement("chain_2", "\u8fde\u9501\u53cd\u5e94");
            if (!pendingBombs.isEmpty()) {
                bombsTriggered += pendingBombs.size();
                unlockAchievement("bomb_user", "\u7206\u7834\u4e13\u5bb6");
            }
            if (!pendingBombs.isEmpty()) {
                sound.playExplosion();
            } else {
                sound.playClear();
            }
            invalidate();
            handler.postDelayed(() -> finishRemoval(token), REMOVE_DURATION_MS);
        }

        private void finishRemoval(int token) {
        clearedThisTurn = true;
            if (token != moveToken || !removing) {
                return;
            }
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (pendingRemove[row][col]) {
                        board[row][col] = EMPTY;
                    }
                }
            }
            score += pendingLineScore + pendingBlastCount;
            removedCount += pendingLineCount + pendingBlastCount;
            lineClearCount++;
            bestClearCount = Math.max(bestClearCount, pendingLineCount + pendingBlastCount);
            unlockAchievement("first_clear", "\u521d\u6b21\u8fde\u7ebf");
            if (pendingLineCount + pendingBlastCount >= 8) {
                unlockAchievement("big_clear", "\u4e00\u7f51\u6253\u5c3d");
            }
            if (score >= 50) unlockAchievement("score_50", "\u6e10\u5165\u4f73\u5883");
            if (score >= 100) unlockAchievement("score_100", "\u767e\u5206\u8fbe\u4eba");
            if (dailyChallenge && score > dailyBestScore) {
                dailyBestScore = score;
                settings.edit().putInt("daily_best_" + dateKey(), dailyBestScore).apply();
            }
            removing = false;
            clearPendingRemoval();
            beginResolution(token);
            invalidate();
        }

        private boolean prepareRemovalPlan() {
            pendingRemove = new boolean[SIZE][SIZE];
            pendingLine = new boolean[SIZE][SIZE];
            pendingBlast = new boolean[SIZE][SIZE];
            pendingBombs.clear();
            boolean[] bombColors = new boolean[NORMAL_COLORS + 1];
            int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    int type = board[row][col];
                    if (type < 1 || type > NORMAL_COLORS) {
                        continue;
                    }
                    for (int[] direction : directions) {
                        ArrayList<int[]> line = collectLine(
                                row,
                                col,
                                direction[0],
                                direction[1],
                                type
                        );
                        if (line.size() < 5) {
                            continue;
                        }
                        mark(pendingLine, line);
                        boolean hasBomb = false;
                        for (int[] cell : line) {
                            if (board[cell[0]][cell[1]] == BOMB) {
                                hasBomb = true;
                                break;
                            }
                        }
                        if (hasBomb) {
                            bombColors[type] = true;
                        }
                    }
                }
            }

            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (board[row][col] != BOMB) {
                        continue;
                    }
                    for (int color = 1; color <= NORMAL_COLORS; color++) {
                        for (int[] direction : directions) {
                            ArrayList<int[]> line = collectBombLine(
                                    row,
                                    col,
                                    color,
                                    direction[0],
                                    direction[1]
                            );
                            if (line.size() >= 5 && hasOrdinaryColor(line, color)) {
                                mark(pendingLine, line);
                                bombColors[color] = true;
                            }
                        }
                    }
                }
            }

            for (int color = 1; color <= NORMAL_COLORS; color++) {
                if (!bombColors[color]) {
                    continue;
                }
                for (int row = 0; row < SIZE; row++) {
                    for (int col = 0; col < SIZE; col++) {
                        if (board[row][col] == color && !pendingLine[row][col]) {
                            pendingBlast[row][col] = true;
                        }
                    }
                }
            }

            pendingLineCount = 0;
            pendingBlastCount = 0;
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (pendingLine[row][col]) {
                        pendingRemove[row][col] = true;
                        pendingLineCount++;
                        if (board[row][col] == BOMB) {
                            pendingBombs.add(new int[]{row, col});
                        }
                    } else if (pendingBlast[row][col]) {
                        pendingRemove[row][col] = true;
                        pendingBlastCount++;
                    }
                }
            }
            pendingLineScore = pendingLineCount >= 5
                    ? 5 + (pendingLineCount - 5) * 2
                    : 0;
            return pendingLineCount > 0 || pendingBlastCount > 0;
        }

        private boolean hasOrdinaryColor(ArrayList<int[]> line, int color) {
            for (int[] cell : line) {
                if (board[cell[0]][cell[1]] == color) {
                    return true;
                }
            }
            return false;
        }

        private ArrayList<int[]> collectLine(
                int row,
                int col,
                int rowStep,
                int colStep,
                int color
        ) {
            ArrayList<int[]> line = new ArrayList<>();
            line.add(new int[]{row, col});
            int nextRow = row + rowStep;
            int nextCol = col + colStep;
            while (inside(nextRow, nextCol)
                    && matchesLineColor(board[nextRow][nextCol], color)) {
                line.add(new int[]{nextRow, nextCol});
                nextRow += rowStep;
                nextCol += colStep;
            }
            nextRow = row - rowStep;
            nextCol = col - colStep;
            while (inside(nextRow, nextCol)
                    && matchesLineColor(board[nextRow][nextCol], color)) {
                line.add(new int[]{nextRow, nextCol});
                nextRow -= rowStep;
                nextCol -= colStep;
            }
            return line;
        }

        private ArrayList<int[]> collectBombLine(
                int bombRow,
                int bombCol,
                int color,
                int rowStep,
                int colStep
        ) {
            ArrayList<int[]> line = new ArrayList<>();
            line.add(new int[]{bombRow, bombCol});
            int nextRow = bombRow + rowStep;
            int nextCol = bombCol + colStep;
            while (inside(nextRow, nextCol)
                    && matchesLineColor(board[nextRow][nextCol], color)) {
                line.add(new int[]{nextRow, nextCol});
                nextRow += rowStep;
                nextCol += colStep;
            }
            nextRow = bombRow - rowStep;
            nextCol = bombCol - colStep;
            while (inside(nextRow, nextCol)
                    && matchesLineColor(board[nextRow][nextCol], color)) {
                line.add(new int[]{nextRow, nextCol});
                nextRow -= rowStep;
                nextCol -= colStep;
            }
            return line;
        }

        private boolean matchesLineColor(int value, int color) {
            return value == color || value == WHITE || value == BOMB;
        }

        private void mark(boolean[][] target, ArrayList<int[]> cells) {
            for (int[] cell : cells) {
                target[cell[0]][cell[1]] = true;
            }
        }

        private float removalProgress() {
            return Math.max(
                    0f,
                    Math.min(1f, (SystemClock.uptimeMillis() - removalStart) / (float) REMOVE_DURATION_MS)
            );
        }

        private void clearPendingRemoval() {
            pendingRemove = new boolean[SIZE][SIZE];
            pendingLine = new boolean[SIZE][SIZE];
            pendingBlast = new boolean[SIZE][SIZE];
            pendingBombs.clear();
            pendingLineCount = 0;
            pendingBlastCount = 0;
            pendingLineScore = 0;
        }

        private boolean inside(int row, int col) {
            return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
        }

        private boolean isFull() {
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (board[row][col] == EMPTY) {
                        return false;
                    }
                }
            }
            return true;
        }

        private void finishGame() {
            if (gameOver) {
                return;
            }
            gameOver = true;
            recordCompletedGame();
            if (moveCount >= 30) unlockAchievement("patient_30", "\u6df1\u8c0b\u8fdc\u8651");
            invalidate();
            handler.postDelayed(this::showGameOverDialog, 120L);
        }

        private boolean tryStartCornerEasterEgg() {
            if (claimedEaster != EASTER_NONE || easterKind != EASTER_NONE || easterLoading || gameOver) {
                return false;
            }
            if (cornersAre(COLOR_CYAN)) {
                startCornerEasterEgg(EASTER_RAY);
                return true;
            }
            if (cornersAre(COLOR_PINK)) {
                startCornerEasterEgg(EASTER_MOLLY);
                return true;
            }
            return false;
        }

        private boolean cornersAre(int type) {
            return board[0][0] == type
                    && board[0][SIZE - 1] == type
                    && board[SIZE - 1][0] == type
                    && board[SIZE - 1][SIZE - 1] == type;
        }

        private void startCornerEasterEgg(int kind) {
            if (claimedEaster != EASTER_NONE) return;
            moveToken++;
            handler.removeCallbacksAndMessages(null);
            clearPendingRemoval();
            clearSpawnedCells();
            for (int[] row : board) {
                Arrays.fill(row, EMPTY);
            }
            Arrays.fill(preview, EMPTY);
            clearSpecialModes(true);
            selectedRow = -1;
            selectedCol = -1;
            moving = false;
            spawning = false;
            removing = false;
            spawnedThisTurn = false;
            clearedThisTurn = false;
            racerTrailCells.clear();
            racerTrailType = EMPTY;
            racerTrailUntil = 0L;
            easterKind = kind;
            claimedEaster = kind;
            sound.updateMusic(musicEnabled, musicVolume, activeMusicTrack());
            easterLoading = true;
            easterSecretTaps = 0;
            easterStart = SystemClock.uptimeMillis();
            sound.playClear();
            scheduleEasterLoadingAlarms(kind);
            invalidate();
            handler.postDelayed(() -> showEasterReadyDialog(kind), easterLoadingDurationFor(kind));
        }

        private void scheduleEasterLoadingAlarms(int kind) {
            final int token = moveToken;
            int count = easterLoadingColorCount(kind);
            for (int phase = 0; phase < count; phase++) {
                final int scheduledPhase = phase;
                handler.postDelayed(() -> {
                    if (token == moveToken && easterKind == kind && easterLoading) {
                        sound.playAlarm();
                    }
                }, scheduledPhase * EASTER_FLASH_FULL_MS);
            }
        }

        private long easterLoadingDurationFor(int kind) {
            return EASTER_FLASH_FULL_MS * easterLoadingColorCount(kind);
        }

        private int easterLoadingColorCount(int kind) {
            return kind == EASTER_TOUTOU ? TOUTOU_LOADING_COLORS : NORMAL_COLORS;
        }

        private void drawEasterLoading(Canvas canvas) {
            long elapsed = Math.max(0L, SystemClock.uptimeMillis() - easterStart);
            int colorCount = easterLoadingColorCount(easterKind);
            int halfCycle = (int) Math.min(colorCount * 2L - 1L, elapsed / EASTER_FLASH_HALF_MS);
            int alpha = halfCycle % 2 == 0 ? 248 : 68;
            int colorPhase = (int) Math.min(colorCount - 1L, elapsed / EASTER_FLASH_FULL_MS);
            int type = colorPhase + 1;
            String title = easterKind == EASTER_TOUTOU
                    ? "头头"
                    : (easterKind == EASTER_MOLLY ? "Molly" : "Ray");
            String subtitle = easterKind == EASTER_TOUTOU ? "1314" : "loading...";
            int titleColumns = glyphColumns(title);
            int subtitleColumns = glyphColumns(subtitle);
            int titleRows = glyphRows(title);
            int subtitleRows = glyphRows(subtitle);
            float titleStep = Math.min(cellSize * 0.74f, boardSize / Math.max(1f, titleColumns + 2.0f));
            float subtitleStep = Math.min(cellSize * 0.36f, boardSize / Math.max(1f, subtitleColumns + 3.0f));
            if (easterKind == EASTER_TOUTOU) {
                titleStep = Math.min(cellSize * 0.62f, boardSize / Math.max(1f, titleColumns + 2.0f));
                subtitleStep = Math.min(cellSize * 0.50f, boardSize / Math.max(1f, subtitleColumns + 3.0f));
            }
            float totalHeight = titleStep * titleRows + subtitleStep * 1.55f + subtitleStep * subtitleRows;
            float titleY = boardTop + (boardSize - totalHeight) * 0.50f;
            float subtitleY = titleY + titleStep * titleRows + subtitleStep * 1.55f;
            drawLoadingLine(canvas, title, titleStep, titleY, Math.max(3.2f, titleStep * 0.31f), type, alpha);
            drawLoadingLine(canvas, subtitle, subtitleStep, subtitleY, Math.max(2.0f, subtitleStep * 0.30f), type, alpha);
        }

        private void drawLoadingLine(
                Canvas canvas,
                String message,
                float step,
                float startY,
                float radius,
                int type,
                int alpha
        ) {
            int columns = glyphColumns(message);
            float startX = boardLeft + (boardSize - Math.max(0, columns - 1) * step) * 0.5f;
            float cursorX = startX;
            for (int charIndex = 0; charIndex < message.length(); charIndex++) {
                String[] glyph = glyphFor(message.charAt(charIndex));
                for (int row = 0; row < glyph.length; row++) {
                    for (int col = 0; col < glyph[row].length(); col++) {
                        if (glyph[row].charAt(col) != '#') {
                            continue;
                        }
                        drawLoadingDot(
                                canvas,
                                type,
                                cursorX + col * step,
                                startY + row * step,
                                radius,
                                alpha
                        );
                    }
                }
                cursorX += (glyph[0].length() + 1) * step;
            }
        }
        private int glyphColumns(String message) {
            int columns = 0;
            for (int i = 0; i < message.length(); i++) {
                columns += glyphFor(message.charAt(i))[0].length() + 1;
            }
            return Math.max(0, columns - 1);
        }

        private int glyphRows(String message) {
            int rows = 1;
            for (int i = 0; i < message.length(); i++) {
                rows = Math.max(rows, glyphFor(message.charAt(i)).length);
            }
            return rows;
        }

        private String[] glyphFor(char value) {
            switch (Character.toLowerCase(value)) {
                case '1':
                    return new String[]{" # ", "## ", " # ", " # ", "###"};
                case '3':
                    return new String[]{"## ", "  #", " # ", "  #", "## "};
                case '4':
                    return new String[]{"# #", "# #", "###", "  #", "  #"};
                case 'a':
                    return new String[]{" # ", "# #", "###", "# #", "# #"};
                case 'd':
                    return new String[]{"## ", "# #", "# #", "# #", "## "};
                case 'g':
                    return new String[]{" ##", "#  ", "# #", "# #", " ##"};
                case 'i':
                    return new String[]{"###", " # ", " # ", " # ", "###"};
                case 'l':
                    return new String[]{"#  ", "#  ", "#  ", "#  ", "###"};
                case 'm':
                    return new String[]{"# #", "###", "###", "# #", "# #"};
                case 'n':
                    return new String[]{"## ", "# #", "# #", "# #", "# #"};
                case 'o':
                    return new String[]{" # ", "# #", "# #", "# #", " # "};
                case 'r':
                    return new String[]{"## ", "# #", "## ", "# #", "# #"};
                case 'y':
                    return new String[]{"# #", "# #", " # ", " # ", " # "};
                case '头':
                    return new String[]{
                            "   #   ",
                            "#  #  #",
                            " # # # ",
                            "  ###  ",
                            "   #   ",
                            "  # #  ",
                            "##   ##"
                    };
                case '.':
                    return new String[]{" ", " ", " ", " ", "#"};
                default:
                    return new String[]{"  ", "  ", "  ", "  ", "  "};
            }
        }

        private void drawLoadingDot(Canvas canvas, int type, float cx, float cy, float radius, int alpha) {
            int baseColor = colorFor(type);
            paint.setShader(new RadialGradient(
                    cx - radius * 0.25f,
                    cy - radius * 0.30f,
                    radius * 1.4f,
                    new int[]{lighten(baseColor, 0.28f), baseColor, darken(baseColor, 0.50f)},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
            ));
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(alpha);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);
            paint.setAlpha(Math.min(255, alpha + 10));
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx - radius * 0.28f, cy - radius * 0.32f, radius * 0.16f, paint);
            paint.setAlpha(255);
        }

        private void showEasterReadyDialog(int kind) {
            if (easterKind != kind || !easterLoading) {
                return;
            }
            easterLoading = false;
            invalidate();

            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER_HORIZONTAL);
            int padding = dp(18);
            root.setPadding(padding, dp(10), padding, dp(4));

            LinearLayout textRow = new LinearLayout(context);
            textRow.setOrientation(LinearLayout.HORIZONTAL);
            textRow.setGravity(Gravity.CENTER);
            char secret = kind == EASTER_MOLLY ? 'y' : 'A';
            String message = "Are you ready?";
            for (int i = 0; i < message.length(); i++) {
                char value = message.charAt(i);
                TextView letter = new TextView(context);
                letter.setText(String.valueOf(value));
                letter.setTextSize(27f);
                letter.setGravity(Gravity.CENTER);
                letter.setPadding(dp(1), dp(8), dp(1), dp(8));
                if (value == secret) {
                    letter.setTextColor(kind == EASTER_MOLLY ? 0xffff1493 : 0xff00b4d8);
                    letter.setOnClickListener(v -> handleEasterSecretTap(kind));
                } else {
                    letter.setTextColor(0xff192624);
                }
                textRow.addView(letter);
            }
            root.addView(textRow);

            easterDialog = new AlertDialog.Builder(context)
                    .setTitle(easterDialogTitle(kind))
                    .setView(root)
                    .setPositiveButton("ok", (dialog, which) -> finishEaster(false))
                    .setCancelable(false)
                    .create();
            easterDialog.show();
        }

        private String easterDialogTitle(int kind) {
            if (kind == EASTER_TOUTOU) {
                return "头头 1314";
            }
            return kind == EASTER_MOLLY ? "Molly loading..." : "Ray loading...";
        }

        private void handleEasterSecretTap(int kind) {
            if (easterKind != kind) {
                return;
            }
            easterSecretTaps++;
            sound.playClick();
            if (easterSecretTaps >= requiredEasterSecretTaps(kind)) {
                finishEaster(true);
            }
        }

        private int requiredEasterSecretTaps(int kind) {
            return kind == EASTER_TOUTOU ? 13 : 5;
        }

        private void finishEaster(boolean hidden) {
            int kind = easterKind;
            if (hidden) {
                activateHiddenEaster(kind);
            }
            if (easterDialog != null) {
                AlertDialog dialog = easterDialog;
                easterDialog = null;
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
            easterKind = EASTER_NONE;
            easterLoading = false;
            easterSecretTaps = 0;
            if (!hidden && kind == EASTER_TOUTOU) {
                exitGame();
                return;
            }
            selectedRow = -1;
            selectedCol = -1;
            preparePreview();
            spawnPieces();
            invalidate();
        }

        private void activateHiddenEaster(int kind) {
            if (kind == EASTER_RAY) {
                rayRacerMode = true;
            } else if (kind == EASTER_MOLLY) {
                kuromiTheme = true;
            } else if (kind == EASTER_TOUTOU) {
                heartMode = true;
                sound.updateMusic(musicEnabled, musicVolume, activeMusicTrack());
            }
            sound.playClear();
        }

        private void exitGame() {
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        }

        private void closeEasterDialog() {
            if (easterDialog == null) {
                return;
            }
            AlertDialog dialog = easterDialog;
            easterDialog = null;
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

        private void showGameOverDialog() {
            if (dialogShowing) {
                return;
            }
            dialogShowing = true;
            if (qualifiesForHighScore(score)) {
                final EditText nameInput = new EditText(context);
                nameInput.setHint("\u8bf7\u8f93\u5165\u6635\u79f0");
                nameInput.setSingleLine(true);
                nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);
                int padding = dp(22);
                layout.setPadding(padding, 0, padding, 0);
                TextView message = new TextView(context);
                message.setText(gameSummary() + "\n\u606d\u559c\u8fdb\u5165\u524d10\uff0c\u8bf7\u7559\u4e0b\u6635\u79f0");
                message.setTextSize(17f);
                layout.addView(message);
                layout.addView(nameInput);
                new AlertDialog.Builder(context)
                        .setTitle("\u6e38\u620f\u7ed3\u675f")
                        .setView(layout)
                        .setPositiveButton("\u4fdd\u5b58\u5e76\u65b0\u6e38\u620f", (d, which) -> {
                            saveHighScore(nameInput.getText().toString(), score);
                            dialogShowing = false;
                            resetGame(dailyChallenge);
                        })
                        .setNegativeButton("\u8df3\u8fc7\u5e76\u65b0\u6e38\u620f", (d, which) -> {
                            dialogShowing = false;
                            resetGame(dailyChallenge);
                        })
                        .setCancelable(false)
                        .show();
            } else {
                new AlertDialog.Builder(context)
                        .setTitle("\u6e38\u620f\u7ed3\u675f")
                        .setMessage("\u68cb\u76d8\u5df2\u586b\u6ee1\n" + gameSummary())
                        .setPositiveButton("\u91cd\u65b0\u5f00\u59cb", (dialog, which) -> {
                            dialogShowing = false;
                            resetGame(dailyChallenge);
                        })
                        .setCancelable(false)
                        .show();
            }
        }

        private void showMenu() {
            String[] items = {
                    "\u65b0\u6e38\u620f", "\u6bcf\u65e5\u6311\u6218", "\u64a4\u9500\u4e00\u6b65", "\u65b0\u624b\u5f15\u5bfc",
                    "\u9ad8\u5206\u699c", "\u5386\u53f2\u6218\u7ee9", "\u6210\u5c31", "\u8bbe\u7f6e"
            };
            new AlertDialog.Builder(context)
                    .setTitle("\u83dc\u5355")
                    .setItems(items, (dialog, which) -> {
                        if (which == 0) {
                            resetGame();
                        } else if (which == 1) {
                            resetGame(true);
                        } else if (which == 2) {
                            undoLastMove();
                        } else if (which == 3) {
                            beginTutorial();
                        } else if (which == 4) {
                            showHighScores();
                        } else if (which == 5) {
                            showHistory();
                        } else if (which == 6) {
                            showAchievements();
                        } else {
                            showSettings();
                        }
                    })
                    .show();
        }

        private String gameSummary() {
            return String.format(
                    Locale.US,
                    "%s\n\u672c\u5c40\u5f97\u5206\uff1a%d\n\u79fb\u52a8\uff1a%d \u6b21\n\u6d88\u9664\uff1a%d \u4e2a\n\u5b8c\u6210\u8fde\u7ebf\uff1a%d \u6b21\n\u5355\u6b21\u6700\u591a\u6d88\u9664\uff1a%d \u4e2a\n\u6700\u9ad8\u8fde\u9501\uff1a%d \u6b21\n\u5f15\u7206\u70b8\u836f\uff1a%d \u4e2a",
                    dailyChallenge ? "\u6bcf\u65e5\u6311\u6218" : "\u666e\u901a\u6a21\u5f0f",
                    score,
                    moveCount,
                    removedCount,
                    lineClearCount,
                    bestClearCount,
                    bestChain,
                    bombsTriggered
            );
        }

        private void beginTutorial() {
            if (moving || spawning || removing || gameOver) {
                handler.postDelayed(this::beginTutorial, 180L);
                return;
            }
            int[][] steps = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    if (board[row][col] == EMPTY) continue;
                    for (int[] step : steps) {
                        int targetRow = row + step[0];
                        int targetCol = col + step[1];
                        if (inside(targetRow, targetCol) && board[targetRow][targetCol] == EMPTY) {
                            tutorialSource = row * SIZE + col;
                            tutorialTarget = targetRow * SIZE + targetCol;
                            tutorialStep = 0;
                            selectedRow = -1;
                            selectedCol = -1;
                            invalidate();
                            return;
                        }
                    }
                }
            }
        }

        private void finishTutorial() {
            tutorialStep = -1;
            tutorialSource = -1;
            tutorialTarget = -1;
            settings.edit().putBoolean("tutorial_seen", true).apply();
            sound.playClick();
            invalidate();
        }

        private void captureUndoState() {
            undoBoard = new int[SIZE][SIZE];
            for (int row = 0; row < SIZE; row++) {
                System.arraycopy(board[row], 0, undoBoard[row], 0, SIZE);
            }
            undoPreview = Arrays.copyOf(preview, preview.length);
            undoScore = score;
            undoRemovedCount = removedCount;
            undoMoveCount = moveCount;
            undoLineClearCount = lineClearCount;
            undoBestClearCount = bestClearCount;
            undoBestChain = bestChain;
            undoBombsTriggered = bombsTriggered;
            undoDailyRandomState = dailyRandomState;
            undoAvailable = true;
        }

        private void undoLastMove() {
            if (!undoAvailable || moving || spawning || removing || gameOver || undoBoard == null) {
                sound.playClick();
                return;
            }
            moveToken++;
            for (int row = 0; row < SIZE; row++) {
                System.arraycopy(undoBoard[row], 0, board[row], 0, SIZE);
            }
            System.arraycopy(undoPreview, 0, preview, 0, preview.length);
            score = undoScore;
            removedCount = undoRemovedCount;
            moveCount = undoMoveCount;
            lineClearCount = undoLineClearCount;
            bestClearCount = undoBestClearCount;
            bestChain = undoBestChain;
            bombsTriggered = undoBombsTriggered;
            currentChain = 0;
            dailyRandomState = undoDailyRandomState;
            selectedRow = -1;
            selectedCol = -1;
            moving = false;
            spawning = false;
            removing = false;
            spawnedThisTurn = false;
            clearedThisTurn = false;
            undoAvailable = false;
            undoBoard = null;
            clearPendingRemoval();
            clearSpawnedCells();
            sound.playClick();
            invalidate();
        }

        private void showHighScores() {
            ArrayList<ScoreEntry> entries = loadHighScores();
            StringBuilder text = new StringBuilder();
            if (entries.isEmpty()) {
                text.append("\u6682\u65e0\u8bb0\u5f55");
            } else {
                for (int i = 0; i < entries.size(); i++) {
                    ScoreEntry entry = entries.get(i);
                    text.append(String.format(
                            Locale.US,
                            "%2d. %-12s %05d",
                            i + 1,
                            entry.name,
                            entry.score
                    ));
                    if (i + 1 < entries.size()) {
                        text.append('\n');
                    }
                }
            }
            TextView view = new TextView(context);
            view.setText(text.toString());
            view.setTextSize(18f);
            view.setTypeface(Typeface.MONOSPACE);
            view.setPadding(dp(22), dp(8), dp(22), dp(8));
            new AlertDialog.Builder(context)
                    .setTitle("\u9ad8\u5206\u699c\uff08\u524d10\u540d\uff09")
                    .setView(view)
                    .setPositiveButton("\u5173\u95ed", null)
                    .show();
        }

        private void showHistory() {
            SharedPreferences prefs = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE);
            int count = Math.min(50, prefs.getInt("count", 0));
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < count; i++) {
                String record = prefs.getString("record_" + i, "");
                String[] fields = record.split("\\|", -1);
                if (fields.length != 5) continue;
                if (text.length() > 0) text.append('\n').append('\n');
                text.append(fields[0]).append("  ")
                        .append("1".equals(fields[1]) ? "\u6bcf\u65e5\u6311\u6218" : "\u666e\u901a\u6a21\u5f0f")
                        .append('\n')
                        .append(String.format(Locale.US, "\u5f97\u5206 %05d  \u79fb\u52a8 %s  \u6d88\u9664 %s",
                                Integer.parseInt(fields[2]), fields[3], fields[4]));
            }
            if (text.length() == 0) text.append("\u6682\u65e0\u5386\u53f2\u6218\u7ee9");
            TextView view = new TextView(context);
            view.setText(text.toString());
            view.setTextSize(16f);
            view.setPadding(dp(22), dp(8), dp(22), dp(8));
            ScrollView scroll = new ScrollView(context);
            scroll.addView(view);
            new AlertDialog.Builder(context)
                    .setTitle("\u5386\u53f2\u6218\u7ee9\uff08\u6700\u8fd150\u5c40\uff09")
                    .setView(scroll)
                    .setPositiveButton("\u5173\u95ed", null)
                    .show();
        }

        private void showAchievements() {
            String[][] achievements = {
                    {"first_clear", "\u521d\u6b21\u8fde\u7ebf", "\u5b8c\u6210\u7b2c\u4e00\u6b21\u4e94\u5b50\u6d88\u9664"},
                    {"big_clear", "\u4e00\u7f51\u6253\u5c3d", "\u5355\u6b21\u6d88\u9664\u81f3\u5c118\u4e2a\u68cb\u5b50"},
                    {"chain_2", "\u8fde\u9501\u53cd\u5e94", "\u4e00\u6b21\u884c\u52a8\u89e6\u53d1\u81f3\u5c112\u6b21\u8fde\u7eed\u6d88\u9664"},
                    {"bomb_user", "\u7206\u7834\u4e13\u5bb6", "\u6210\u529f\u5f15\u7206\u4e00\u6b21\u70b8\u836f"},
                    {"score_50", "\u6e10\u5165\u4f73\u5883", "\u5355\u5c40\u8fbe\u523050\u5206"},
                    {"score_100", "\u767e\u5206\u8fbe\u4eba", "\u5355\u5c40\u8fbe\u5230100\u5206"},
                    {"patient_30", "\u6df1\u8c0b\u8fdc\u8651", "\u5355\u5c40\u79fb\u52a8\u81f3\u5c1130\u6b21"},
                    {"daily_player", "\u4eca\u65e5\u6311\u6218", "\u5f00\u59cb\u4e00\u6b21\u6bcf\u65e5\u6311\u6218"}
            };
            Set<String> unlocked = new HashSet<>(context
                    .getSharedPreferences(ACHIEVEMENT_PREFS, Context.MODE_PRIVATE)
                    .getStringSet("unlocked", Collections.emptySet()));
            StringBuilder text = new StringBuilder();
            for (String[] achievement : achievements) {
                text.append(unlocked.contains(achievement[0]) ? "[x] " : "[ ] ")
                        .append(achievement[1]).append('\n')
                        .append("    ").append(achievement[2]).append('\n').append('\n');
            }
            TextView view = new TextView(context);
            view.setText(text.toString().trim());
            view.setTextSize(17f);
            view.setPadding(dp(22), dp(8), dp(22), dp(8));
            new AlertDialog.Builder(context)
                    .setTitle(String.format(Locale.US, "\u6210\u5c31 %d/%d", unlocked.size(), achievements.length))
                    .setView(view)
                    .setPositiveButton("\u5173\u95ed", null)
                    .show();
        }

        private void recordCompletedGame() {
            if (gameRecorded) return;
            gameRecorded = true;
            SharedPreferences prefs = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE);
            int oldCount = Math.min(49, prefs.getInt("count", 0));
            SharedPreferences.Editor editor = prefs.edit();
            for (int i = oldCount; i > 0; i--) {
                editor.putString("record_" + i, prefs.getString("record_" + (i - 1), ""));
            }
            String record = displayDate() + "|" + (dailyChallenge ? "1" : "0") + "|"
                    + score + "|" + moveCount + "|" + removedCount;
            editor.putString("record_0", record).putInt("count", oldCount + 1).apply();
        }

        private void unlockAchievement(String id, String title) {
            SharedPreferences prefs = context.getSharedPreferences(ACHIEVEMENT_PREFS, Context.MODE_PRIVATE);
            Set<String> unlocked = new HashSet<>(prefs.getStringSet("unlocked", Collections.emptySet()));
            if (!unlocked.add(id)) return;
            prefs.edit().putStringSet("unlocked", unlocked).apply();
            achievementToast = "\u6210\u5c31\u89e3\u9501\uff1a" + title;
            achievementToastUntil = SystemClock.uptimeMillis() + 2200L;
            invalidate();
        }

        private String dateKey() {
            Calendar calendar = Calendar.getInstance();
            return String.format(Locale.US, "%04d%02d%02d",
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH));
        }

        private long dailySeed() {
            try {
                return Long.parseLong(dateKey()) & 0xffffffffL;
            } catch (NumberFormatException ignored) {
                return 1L;
            }
        }

        private String displayDate() {
            Calendar calendar = Calendar.getInstance();
            return String.format(Locale.US, "%04d-%02d-%02d %02d:%02d",
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE));
        }

        private void showSettings() {
            ScrollView scroll = new ScrollView(context);
            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            int padding = dp(18);
            root.setPadding(padding, dp(4), padding, dp(4));
            scroll.addView(root);

            TextView progressionNote = new TextView(context);
            progressionNote.setText("\u6bcf\u5b8c\u621012\u6b21\u6709\u6548\u79fb\u52a8\uff0c\u6bcf\u56de\u5408\u4f1a\u591a\u751f\u62101\u4e2a\u68cb\u5b50\uff0c\u6700\u591a\u989d\u5916\u751f\u62103\u4e2a\u3002");
            progressionNote.setTextSize(14f);
            progressionNote.setTextColor(0xff6b7280);
            progressionNote.setPadding(0, dp(4), 0, dp(10));
            root.addView(progressionNote);

            settingsMusicToggleCount = 0;
            final TextView adminNote = new TextView(context);
            adminNote.setText(adminModeNoteText());
            adminNote.setTextSize(14f);
            adminNote.setTextColor(adminMode ? 0xff14532d : 0xff6b7280);
            adminNote.setPadding(0, dp(4), 0, dp(10));
            root.addView(adminNote);

            final TextView difficultyValue = new TextView(context);
            SeekBar difficulty = addSlider(
                    root,
                    "\u96be\u5ea6\u7cfb\u6570",
                    difficultyValue,
                    10,
                    Math.round((difficultyMultiplier - 1f) * 10f),
                    1f,
                    0.1f
            );
            difficulty.setEnabled(adminMode);
            final TextView whiteValue = new TextView(context);
            SeekBar white = addSlider(
                    root,
                    "\u767d\u68cb\u6982\u7387\u7cfb\u6570",
                    whiteValue,
                    20,
                    Math.round(whiteProbabilityMultiplier * 10f),
                    0f,
                    0.1f
            );
            white.setEnabled(adminMode);
            final TextView bombValue = new TextView(context);
            SeekBar bomb = addSlider(
                    root,
                    "\u70b8\u836f\u6982\u7387\u7cfb\u6570",
                    bombValue,
                    20,
                    Math.round(bombProbabilityMultiplier * 10f),
                    0f,
                    0.1f
            );
            bomb.setEnabled(adminMode);

            final TextView movementSpeedValue = new TextView(context);
            SeekBar movementSpeedBar = addChoiceSlider(
                    root,
                    "\u79fb\u52a8\u901f\u5ea6",
                    movementSpeedValue,
                    new String[]{"\u6162", "\u6b63\u5e38", "\u5feb", "\u5149\u901f"},
                    movementSpeed
            );

            CheckBox music = new CheckBox(context);
            music.setText("\u97f3\u4e50");
            music.setChecked(musicEnabled);
            root.addView(music);
            music.setOnCheckedChangeListener((buttonView, isChecked) -> {
                settingsMusicToggleCount++;
                if (!adminMode && settingsMusicToggleCount >= 7) {
                    adminMode = true;
                    adminUnlockedByMusic = true;
                    settingsMusicToggleCount = 0;
                    difficulty.setEnabled(true);
                    white.setEnabled(true);
                    bomb.setEnabled(true);
                    adminNote.setText("管理员模式已开启：可以调整难度、白棋和炸药概率。");
                    adminNote.setTextColor(0xff14532d);
                    settings.edit().putBoolean("admin_mode", true).apply();
                    sound.playAlarm();
                } else {
                    sound.playClick();
                }
            });

            final TextView musicTrackValue = new TextView(context);
            SeekBar musicTrackBar = addChoiceSlider(
                    root,
                    "\u97f3\u4e50\u66f2\u76ee",
                    musicTrackValue,
                    new String[]{"\u97f3\u4e501", "\u97f3\u4e502"},
                    musicTrack
            );

            final TextView musicVolumeValue = new TextView(context);
            SeekBar musicVolumeBar = addSlider(
                    root,
                    "\u97f3\u4e50\u97f3\u91cf",
                    musicVolumeValue,
                    100,
                    Math.round(musicVolume * 100f),
                    0f,
                    0.01f
            );

            CheckBox effects = new CheckBox(context);
            effects.setText("\u97f3\u6548");
            effects.setChecked(soundEnabled);
            root.addView(effects);
            final TextView soundVolumeValue = new TextView(context);
            SeekBar soundVolumeBar = addSlider(
                    root,
                    "\u97f3\u6548\u97f3\u91cf",
                    soundVolumeValue,
                    100,
                    Math.round(soundVolume * 100f),
                    0f,
                    0.01f
            );

            new AlertDialog.Builder(context)
                    .setTitle("\u8bbe\u7f6e")
                    .setView(scroll)
                    .setPositiveButton("\u4fdd\u5b58", (d, which) -> {
                        boolean wasAdminMode = adminMode;
                        difficultyMultiplier = wasAdminMode
                                ? 1f + difficulty.getProgress() / 10f
                                : difficultyMultiplier;
                        whiteProbabilityMultiplier = wasAdminMode
                                ? white.getProgress() / 10f
                                : whiteProbabilityMultiplier;
                        bombProbabilityMultiplier = wasAdminMode
                                ? bomb.getProgress() / 10f
                                : bombProbabilityMultiplier;
                        movementSpeed = movementSpeedBar.getProgress();
                        musicEnabled = music.isChecked();
                        musicTrack = Math.max(
                                MUSIC_TRACK_SYNTH,
                                Math.min(MUSIC_TRACK_MIDI, musicTrackBar.getProgress())
                        );
                        soundEnabled = effects.isChecked();
                        musicVolume = musicVolumeBar.getProgress() / 100f;
                        soundVolume = soundVolumeBar.getProgress() / 100f;
                        boolean triggerToutou = claimedEaster == EASTER_NONE
                                && wasAdminMode && adminUnlockedByMusic
                                && Math.abs(whiteProbabilityMultiplier - 1.3f) < 0.001f
                                && Math.abs(bombProbabilityMultiplier - 1.4f) < 0.001f;
                        if (wasAdminMode) {
                            adminMode = false;
                            settingsMusicToggleCount = 0;
                        }
                        adminUnlockedByMusic = false;
                        settings.edit()
                                .putFloat("difficulty", difficultyMultiplier)
                                .putFloat("white_probability", whiteProbabilityMultiplier)
                                .putFloat("bomb_probability", bombProbabilityMultiplier)
                                .putBoolean("admin_mode", adminMode)
                                .putInt("movement_speed", movementSpeed)
                                .putBoolean("music_enabled", musicEnabled)
                                .putInt("music_track", musicTrack)
                                .putBoolean("sound_enabled", soundEnabled)
                                .putFloat("music_volume", musicVolume)
                                .putFloat("sound_volume", soundVolume)
                                .apply();
                        sound.updateSound(soundEnabled, soundVolume);
                        sound.updateMusic(musicEnabled, musicVolume, activeMusicTrack());
                        if (triggerToutou) {
                            handler.postDelayed(() -> startCornerEasterEgg(EASTER_TOUTOU), 120L);
                        }
                    })
                    .setNegativeButton("\u53d6\u6d88", null)
                    .show();
        }

        private SeekBar addSlider(
                LinearLayout root,
                String title,
                TextView valueView,
                int max,
                int progress,
                float minimum,
                float step
        ) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.VERTICAL);
            TextView titleView = new TextView(context);
            titleView.setText(title);
            titleView.setTextSize(16f);
            valueView.setTextSize(14f);
            SeekBar bar = new SeekBar(context);
            bar.setMax(max);
            bar.setProgress(Math.max(0, Math.min(max, progress)));
            Runnable update = () -> valueView.setText(String.format(
                    Locale.US,
                    "%.1f",
                    minimum + bar.getProgress() * step
            ));
            update.run();
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    update.run();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            row.addView(titleView);
            row.addView(valueView);
            row.addView(bar);
            root.addView(row);
            return bar;
        }

        private SeekBar addChoiceSlider(
                LinearLayout root,
                String title,
                TextView valueView,
                String[] labels,
                int progress
        ) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.VERTICAL);
            TextView titleView = new TextView(context);
            titleView.setText(title);
            titleView.setTextSize(16f);
            valueView.setTextSize(14f);
            SeekBar bar = new SeekBar(context);
            bar.setMax(Math.max(0, labels.length - 1));
            bar.setProgress(Math.max(0, Math.min(bar.getMax(), progress)));
            Runnable update = () -> valueView.setText(labels[bar.getProgress()]);
            update.run();
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                    update.run();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            row.addView(titleView);
            row.addView(valueView);
            row.addView(bar);
            root.addView(row);
            return bar;
        }

        private ArrayList<ScoreEntry> loadHighScores() {
            SharedPreferences scorePrefs = context.getSharedPreferences(SCORES_PREFS, Context.MODE_PRIVATE);
            ArrayList<ScoreEntry> result = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                if (!scorePrefs.contains("score_" + i)) {
                    continue;
                }
                result.add(new ScoreEntry(
                        scorePrefs.getString("name_" + i, "\u73a9\u5bb6"),
                        scorePrefs.getInt("score_" + i, 0)
                ));
            }
            Collections.sort(result, (a, b) -> Integer.compare(b.score, a.score));
            return result;
        }

        private boolean qualifiesForHighScore(int candidate) {
            ArrayList<ScoreEntry> entries = loadHighScores();
            return entries.size() < 10 || candidate > entries.get(entries.size() - 1).score;
        }

        private void saveHighScore(String rawName, int newScore) {
            String name = rawName == null ? "" : rawName.trim();
            if (name.length() == 0) {
                name = "\u73a9\u5bb6";
            }
            if (name.length() > 12) {
                name = name.substring(0, 12);
            }
            ArrayList<ScoreEntry> entries = loadHighScores();
            entries.add(new ScoreEntry(name, newScore));
            Collections.sort(entries, (a, b) -> Integer.compare(b.score, a.score));
            if (entries.size() > 10) {
                entries.subList(10, entries.size()).clear();
            }
            SharedPreferences.Editor editor = context
                    .getSharedPreferences(SCORES_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .clear();
            for (int i = 0; i < entries.size(); i++) {
                editor.putString("name_" + i, entries.get(i).name);
                editor.putInt("score_" + i, entries.get(i).score);
            }
            editor.apply();
        }

        private int dp(int value) {
            return Math.round(value * getResources().getDisplayMetrics().density);
        }

        private float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        void saveState(Bundle outState) {
            int[] cells = new int[SIZE * SIZE];
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    int value = board[row][col];
                    if (removing && pendingRemove[row][col]) {
                        value = EMPTY;
                    }
                    cells[row * SIZE + col] = value;
                }
            }
            if (moving && !movePath.isEmpty()) {
                int[] last = movePath.get(movePath.size() - 1);
                cells[last[0] * SIZE + last[1]] = movingType;
            }
            int savedScore = score + (removing ? pendingLineScore + pendingBlastCount : 0);
            outState.putIntArray("five_lines_board", cells);
            outState.putIntArray("five_lines_preview", preview.clone());
            outState.putInt("five_lines_score", savedScore);
            outState.putInt("five_lines_removed", removedCount + (removing ? pendingLineCount + pendingBlastCount : 0));
            outState.putInt("five_lines_moves", moveCount);
            outState.putInt("five_lines_line_clears", lineClearCount);
            outState.putInt("five_lines_best_clear", bestClearCount);
            outState.putInt("five_lines_best_chain", bestChain);
            outState.putInt("five_lines_bombs_triggered", bombsTriggered);
            outState.putBoolean("five_lines_daily_challenge", dailyChallenge);
            outState.putLong("five_lines_daily_random_state", dailyRandomState);
            outState.putInt("five_lines_daily_best", dailyBestScore);
            outState.putInt("five_lines_selected_row", selectedRow);
            outState.putInt("five_lines_selected_col", selectedCol);
            outState.putBoolean("five_lines_game_over", gameOver);
        outState.putBoolean("five_lines_spawned_this_turn", spawnedThisTurn);
            outState.putBoolean("five_lines_ray_racer_mode", rayRacerMode);
            outState.putBoolean("five_lines_kuromi_theme", kuromiTheme);
            outState.putBoolean("five_lines_heart_mode", heartMode);
            outState.putInt("five_lines_claimed_easter", claimedEaster);
            outState.putBoolean("five_lines_admin_unlocked_by_music", adminUnlockedByMusic);
        }

        private void restoreState(Bundle state) {
            int[] cells = state.getIntArray("five_lines_board");
            if (cells == null || cells.length != SIZE * SIZE) {
                resetGame();
                return;
            }
            for (int row = 0; row < SIZE; row++) {
                for (int col = 0; col < SIZE; col++) {
                    board[row][col] = cells[row * SIZE + col];
                }
            }
            int[] savedPreview = state.getIntArray("five_lines_preview");
            if (savedPreview != null && savedPreview.length == PREVIEW_COUNT) {
                System.arraycopy(savedPreview, 0, preview, 0, PREVIEW_COUNT);
            } else {
                preparePreview();
            }
        score = state.getInt("five_lines_score", 0);
            removedCount = state.getInt("five_lines_removed", 0);
            moveCount = state.getInt("five_lines_moves", 0);
            lineClearCount = state.getInt("five_lines_line_clears", 0);
            bestClearCount = state.getInt("five_lines_best_clear", 0);
            bestChain = state.getInt("five_lines_best_chain", 0);
            bombsTriggered = state.getInt("five_lines_bombs_triggered", 0);
            currentChain = 0;
            dailyChallenge = state.getBoolean("five_lines_daily_challenge", false);
            dailyRandomState = state.getLong("five_lines_daily_random_state", 0L);
            dailyBestScore = state.getInt("five_lines_daily_best", 0);
            undoAvailable = false;
            undoBoard = null;
            selectedRow = state.getInt("five_lines_selected_row", -1);
            selectedCol = state.getInt("five_lines_selected_col", -1);
            gameOver = state.getBoolean("five_lines_game_over", false);
            spawnedThisTurn = state.getBoolean("five_lines_spawned_this_turn", false);
            rayRacerMode = state.getBoolean("five_lines_ray_racer_mode", rayRacerMode);
            kuromiTheme = state.getBoolean("five_lines_kuromi_theme", kuromiTheme);
            heartMode = state.getBoolean("five_lines_heart_mode", heartMode);
            claimedEaster = state.getInt("five_lines_claimed_easter", EASTER_NONE);
            adminUnlockedByMusic = state.getBoolean("five_lines_admin_unlocked_by_music", false);
            sound.updateMusic(musicEnabled, musicVolume, activeMusicTrack());
            moving = false;
            removing = false;
            dialogShowing = false;
            clearPendingRemoval();
            invalidate();
        }

        @Override
        protected void onDetachedFromWindow() {
            moveToken++;
            handler.removeCallbacksAndMessages(null);
            super.onDetachedFromWindow();
        }

        @Override
        public WindowInsets onApplyWindowInsets(WindowInsets insets) {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                statusInset = bars.top;
                navigationInset = bars.bottom;
            } else {
                statusInset = insets.getSystemWindowInsetTop();
                navigationInset = insets.getSystemWindowInsetBottom();
            }
            requestLayout();
            invalidate();
            return insets;
        }

        private float centerX(int col) {
            return boardLeft + (col + 0.5f) * cellSize;
        }

        private float centerY(int row) {
            return boardTop + (row + 0.5f) * cellSize;
        }

        private float lerp(float from, float to, float progress) {
            return from + (to - from) * progress;
        }

        private float easeInOut(float value) {
            return value * value * (3f - 2f * value);
        }
    }

    static final class ScoreEntry {
        final String name;
        final int score;

        ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }
    }

    static final class SoundEngine {
        private static final int SAMPLE_RATE = 22050;
        private static final int MUSIC_TRACK_SYNTH = 0;
        private static final int MUSIC_TRACK_MIDI = 1;
        private static final int MUSIC_TRACK_SY = 2;
        private static final int MUSIC_TRACK_MOLLY = 3;
        private static final int MUSIC_TRACK_RAY = 4;
        private final Context context;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private boolean soundEnabled = true;
        private float soundVolume = 0.75f;
        private boolean musicEnabled = true;
        private float musicVolume = 0.35f;
        private int musicTrack = MUSIC_TRACK_SYNTH;
        private int musicIndex;
        private Runnable musicRunnable;
        private MediaPlayer midiPlayer;
        private final Random random = new Random();

        SoundEngine(Context context) {
            this.context = context.getApplicationContext();
        }

        void updateSound(boolean enabled, float volume) {
            soundEnabled = enabled;
            soundVolume = Math.max(0f, Math.min(1f, volume));
        }

        void updateMusic(boolean enabled, float volume, int track) {
            int nextTrack = Math.max(MUSIC_TRACK_SYNTH, Math.min(MUSIC_TRACK_RAY, track));
            boolean trackChanged = nextTrack != musicTrack;
            musicEnabled = enabled;
            musicVolume = Math.max(0f, Math.min(1f, volume));
            musicTrack = nextTrack;
            if (musicEnabled) {
                if (trackChanged) {
                    stopMusic();
                }
                startMusic();
            } else {
                stopMusic();
            }
            updateMidiVolume();
        }

        void startMusic() {
            if (musicTrack == MUSIC_TRACK_MIDI) {
                startMidiMusic();
                return;
            }
            if (musicTrack == MUSIC_TRACK_SY) {
                startSyMusic();
                return;
            }
            if (musicTrack == MUSIC_TRACK_MOLLY) {
                startMp3Music(R.raw.lbxx);
                return;
            }
            if (musicTrack == MUSIC_TRACK_RAY) {
                startMp3Music(R.raw.twzd);
                return;
            }
            startSynthMusic();
        }

        private void startSynthMusic() {
            if (musicRunnable != null) {
                return;
            }
            musicRunnable = () -> {
                if (!musicEnabled || musicTrack != MUSIC_TRACK_SYNTH) {
                    return;
                }
                double[] melody = {
                        261.63, 329.63, 392.00, 523.25,
                        392.00, 329.63, 293.66, 329.63,
                        261.63, 329.63, 392.00, 440.00,
                        392.00, 329.63, 293.66, 261.63
                };
                double[] harmony = {130.81, 164.81, 196.00, 220.00};
                int index = musicIndex++ % melody.length;
                playTone(melody[index], 360, musicVolume * 0.13f, true);
                if (index % 4 == 0) {
                    playTone(harmony[(index / 4) % harmony.length], 720, musicVolume * 0.055f, true);
                }
                handler.postDelayed(musicRunnable, 430L);
            };
            handler.post(musicRunnable);
        }

        private void startMidiMusic() {
            startMp3Music(R.raw.midi);
        }

        private void startSyMusic() {
            startMp3Music(R.raw.sy);
        }

        private void startMp3Music(int rawResource) {
            if (!musicEnabled) {
                return;
            }
            try {
                if (midiPlayer == null) {
                    midiPlayer = MediaPlayer.create(context, rawResource);
                    if (midiPlayer == null) {
                        return;
                    }
                    midiPlayer.setLooping(true);
                }
                updateMidiVolume();
                if (!midiPlayer.isPlaying()) {
                    midiPlayer.start();
                }
            } catch (Exception ignored) {
                releaseMidiPlayer();
            }
        }

        void stopMusic() {
            if (musicRunnable != null) {
                handler.removeCallbacks(musicRunnable);
                musicRunnable = null;
            }
            releaseMidiPlayer();
        }

        void release() {
            stopMusic();
            handler.removeCallbacksAndMessages(null);
        }

        private void updateMidiVolume() {
            if (midiPlayer == null) {
                return;
            }
            try {
                midiPlayer.setVolume(musicVolume, musicVolume);
            } catch (Exception ignored) {
            }
        }

        private void releaseMidiPlayer() {
            if (midiPlayer == null) {
                return;
            }
            try {
                midiPlayer.stop();
            } catch (Exception ignored) {
            }
            midiPlayer.release();
            midiPlayer = null;
        }

        void playClick() {
            playTone(660, 55, soundVolume * 0.40f);
        }

        void playMove() {
            playTone(440, 130, soundVolume * 0.28f);
        }

        void playSpawn() {
            playTone(520, 100, soundVolume * 0.22f);
        }

        void playClear() {
            playTone(740, 180, soundVolume * 0.38f);
        }

        void playAlarm() {
            playTone(880, 95, soundVolume * 0.46f);
            handler.postDelayed(() -> playTone(1320, 110, soundVolume * 0.36f), 86L);
        }

        void playExplosion() {
            playTone(72, 390, soundVolume * 0.82f);
            playNoiseBurst(260, soundVolume * 0.58f);
            handler.postDelayed(() -> playTone(155, 260, soundVolume * 0.42f), 85L);
        }

        private void playNoiseBurst(int durationMs, float volume) {
            if (!soundEnabled || volume <= 0.001f) {
                return;
            }
            int sampleCount = Math.max(1, SAMPLE_RATE * durationMs / 1000);
            short[] samples = new short[sampleCount];
            double phase = 0d;
            for (int i = 0; i < sampleCount; i++) {
                float progress = i / (float) sampleCount;
                float attack = Math.min(1f, i / (SAMPLE_RATE * 0.004f));
                float decay = Math.max(0f, 1f - progress);
                float envelope = attack * decay * decay;
                phase += 2d * Math.PI * (80d + 95d * decay) / SAMPLE_RATE;
                double low = Math.sin(phase) * 0.45d;
                double noise = (random.nextDouble() * 2d - 1d) * 0.85d;
                samples[i] = (short) Math.round((low + noise) * envelope * volume * Short.MAX_VALUE);
            }
            playSamples(samples, durationMs);
        }

        private void playTone(double frequency, int durationMs, float volume) {
            playTone(frequency, durationMs, volume, false);
        }

        private void playTone(double frequency, int durationMs, float volume, boolean music) {
            if ((music ? !musicEnabled : !soundEnabled) || volume <= 0.001f) {
                return;
            }
            int sampleCount = Math.max(1, SAMPLE_RATE * durationMs / 1000);
            short[] samples = new short[sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                float progress = i / (float) sampleCount;
                float envelope = Math.min(1f, i / (SAMPLE_RATE * 0.012f));
                envelope *= Math.min(1f, (1f - progress) / 0.16f);
                double wave = Math.sin(2d * Math.PI * frequency * i / SAMPLE_RATE);
                samples[i] = (short) Math.round(wave * envelope * volume * Short.MAX_VALUE);
            }
            playSamples(samples, durationMs);
        }

        private void playSamples(short[] samples, int durationMs) {
            try {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                AudioFormat format = new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build();
                AudioTrack track = new AudioTrack.Builder()
                        .setAudioAttributes(attributes)
                        .setAudioFormat(format)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(samples.length * 2)
                        .build();
                track.write(samples, 0, samples.length);
                track.play();
                handler.postDelayed(() -> {
                    try {
                        track.stop();
                    } catch (Exception ignored) {
                    }
                    track.release();
                }, durationMs + 140L);
            } catch (Exception ignored) {
                // Audio is optional; the game remains fully playable if the
                // emulator or device cannot create an AudioTrack.
            }
        }
    }
}
