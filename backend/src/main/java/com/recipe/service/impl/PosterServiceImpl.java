package com.recipe.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipe.entity.Recipe;
import com.recipe.mapper.RecipeMapper;
import com.recipe.service.PosterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PosterServiceImpl implements PosterService {

    private final RecipeMapper recipeMapper;
    private final ObjectMapper objectMapper;

    private static final int POSTER_WIDTH = 800;
    private static final int POSTER_HEIGHT = 800;
    private static final int PADDING = 40;
    private static final int CORNER_RADIUS = 24;

    private static final Color PRIMARY_COLOR = new Color(230, 126, 34);
    private static final Color BG_COLOR = new Color(255, 250, 245);
    private static final Color TEXT_PRIMARY = new Color(51, 51, 51);
    private static final Color TEXT_SECONDARY = new Color(102, 102, 102);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color INGREDIENT_BG = new Color(255, 247, 237);

    @Override
    public byte[] generateRecipePoster(Long recipeId) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            throw new RuntimeException("食谱不存在");
        }

        BufferedImage poster = new BufferedImage(POSTER_WIDTH, POSTER_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = poster.createGraphics();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            drawBackground(g2d);

            int coverHeight = 420;
            drawCoverImage(g2d, recipe.getCoverImage(), coverHeight);

            int contentY = coverHeight + PADDING;
            contentY = drawTitle(g2d, recipe.getTitle(), contentY);
            contentY = drawMeta(g2d, recipe, contentY);
            contentY = drawIngredients(g2d, recipe.getIngredients(), contentY);
            drawFooter(g2d);

        } catch (Exception e) {
            log.error("生成海报失败", e);
            throw new RuntimeException("生成海报失败", e);
        } finally {
            g2d.dispose();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(poster, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("海报图片编码失败", e);
            throw new RuntimeException("海报图片编码失败", e);
        }
    }

    private void drawBackground(Graphics2D g2d) {
        g2d.setColor(BG_COLOR);
        g2d.fillRect(0, 0, POSTER_WIDTH, POSTER_HEIGHT);
    }

    private void drawCoverImage(Graphics2D g2d, String imageUrl, int height) {
        int imgWidth = POSTER_WIDTH - PADDING * 2;
        int imgX = PADDING;
        int imgY = PADDING;

        try {
            URL url = new URL(imageUrl);
            try (InputStream is = url.openStream()) {
                BufferedImage originalImage = ImageIO.read(is);
                if (originalImage != null) {
                    BufferedImage cropped = cropCenterSquare(originalImage, imgWidth, height);
                    BufferedImage rounded = makeRoundedCorner(cropped, CORNER_RADIUS);
                    g2d.drawImage(rounded, imgX, imgY, null);
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("加载封面图失败，使用默认背景: {}", e.getMessage());
        }

        drawDefaultCover(g2d, imgX, imgY, imgWidth, height);
    }

    private BufferedImage cropCenterSquare(BufferedImage original, int targetWidth, int targetHeight) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        double scale = Math.max((double) targetWidth / originalWidth, (double) targetHeight / originalHeight);
        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);

        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();

        int x = (scaledWidth - targetWidth) / 2;
        int y = (scaledHeight - targetHeight) / 2;

        return scaled.getSubimage(Math.max(0, x), Math.max(0, y),
                Math.min(targetWidth, scaledWidth), Math.min(targetHeight, scaledHeight));
    }

    private BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = output.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));
        g2.setComposite(AlphaComposite.SrcIn);
        g2.drawImage(image, 0, 0, null);
        g2.dispose();

        return output;
    }

    private void drawDefaultCover(Graphics2D g2d, int x, int y, int width, int height) {
        GradientPaint gradient = new GradientPaint(
                x, y, new Color(255, 200, 150),
                x, y + height, new Color(230, 126, 34)
        );
        g2d.setPaint(gradient);
        g2d.fill(new RoundRectangle2D.Float(x, y, width, height, CORNER_RADIUS, CORNER_RADIUS));

        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 80));
        String emoji = "🍳";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = x + (width - fm.stringWidth(emoji)) / 2;
        int textY = y + (height + fm.getAscent()) / 2 - 10;
        g2d.drawString(emoji, textX, textY);
    }

    private int drawTitle(Graphics2D g2d, String title, int startY) {
        g2d.setColor(TEXT_PRIMARY);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
        FontMetrics fm = g2d.getFontMetrics();

        int maxWidth = POSTER_WIDTH - PADDING * 2;
        String displayTitle = title;
        if (fm.stringWidth(title) > maxWidth) {
            StringBuilder sb = new StringBuilder();
            for (char c : title.toCharArray()) {
                if (fm.stringWidth(sb.toString() + c + "...") <= maxWidth) {
                    sb.append(c);
                } else {
                    break;
                }
            }
            displayTitle = sb + "...";
        }

        int textX = (POSTER_WIDTH - fm.stringWidth(displayTitle)) / 2;
        g2d.drawString(displayTitle, textX, startY);

        return startY + fm.getHeight() + 16;
    }

    private int drawMeta(Graphics2D g2d, Recipe recipe, int startY) {
        g2d.setColor(TEXT_SECONDARY);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 18));
        FontMetrics fm = g2d.getFontMetrics();

        String author = recipe.getAuthor() != null ? recipe.getAuthor() : "匿名厨师";
        String cookTime = recipe.getCookTime() != null ? recipe.getCookTime() + "分钟" : "--";
        String difficulty = getDifficultyLabel(recipe.getDifficulty());

        String metaText = String.format("👨‍🍳 %s   ⏱️ %s   📊 %s", author, cookTime, difficulty);
        int textX = (POSTER_WIDTH - fm.stringWidth(metaText)) / 2;
        g2d.drawString(metaText, textX, startY);

        return startY + fm.getHeight() + 20;
    }

    private String getDifficultyLabel(Integer level) {
        if (level == null) return "中等";
        return switch (level) {
            case 1 -> "简单";
            case 2 -> "中等";
            case 3 -> "困难";
            default -> "中等";
        };
    }

    private int drawIngredients(Graphics2D g2d, String ingredientsJson, int startY) {
        List<Map<String, Object>> ingredients = parseIngredients(ingredientsJson);
        if (ingredients == null || ingredients.isEmpty()) {
            return startY;
        }

        int cardX = PADDING;
        int cardWidth = POSTER_WIDTH - PADDING * 2;

        int itemHeight = 36;
        int maxItems = 5;
        int displayCount = Math.min(ingredients.size(), maxItems);
        int cardHeight = 50 + displayCount * itemHeight + (ingredients.size() > maxItems ? 30 : 10);

        g2d.setColor(CARD_BG);
        g2d.fill(new RoundRectangle2D.Float(cardX, startY, cardWidth, cardHeight, CORNER_RADIUS, CORNER_RADIUS));

        g2d.setColor(PRIMARY_COLOR);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2d.drawString("🥬 食材概览", cardX + 20, startY + 32);

        int itemY = startY + 60;
        for (int i = 0; i < displayCount; i++) {
            Map<String, Object> item = ingredients.get(i);
            String name = (String) item.getOrDefault("name", "");
            String amount = (String) item.getOrDefault("amount", "");

            int ingredientBoxY = itemY + i * itemHeight;

            g2d.setColor(INGREDIENT_BG);
            g2d.fill(new RoundRectangle2D.Float(cardX + 20, ingredientBoxY, cardWidth - 40, itemHeight - 8, 12, 12));

            g2d.setColor(TEXT_PRIMARY);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g2d.drawString("• " + name, cardX + 36, ingredientBoxY + 20);

            g2d.setColor(PRIMARY_COLOR);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            FontMetrics fm = g2d.getFontMetrics();
            int amountX = cardX + cardWidth - 36 - fm.stringWidth(amount);
            g2d.drawString(amount, amountX, ingredientBoxY + 20);
        }

        if (ingredients.size() > maxItems) {
            g2d.setColor(TEXT_SECONDARY);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 14));
            String moreText = String.format("还有 %d 种食材...", ingredients.size() - maxItems);
            FontMetrics fm = g2d.getFontMetrics();
            int textX = cardX + (cardWidth - fm.stringWidth(moreText)) / 2;
            g2d.drawString(moreText, textX, startY + cardHeight - 12);
        }

        return startY + cardHeight + 20;
    }

    private List<Map<String, Object>> parseIngredients(String ingredientsJson) {
        if (ingredientsJson == null || ingredientsJson.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(ingredientsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("解析食材数据失败", e);
            return null;
        }
    }

    private void drawFooter(Graphics2D g2d) {
        int footerY = POSTER_HEIGHT - 50;

        g2d.setColor(TEXT_SECONDARY);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
        String footerText = "扫码查看完整食谱 · 美食分享平台";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (POSTER_WIDTH - fm.stringWidth(footerText)) / 2;
        g2d.drawString(footerText, textX, footerY);

        g2d.setColor(PRIMARY_COLOR);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        String brandText = "🍽️ 食谱分享";
        int brandX = (POSTER_WIDTH - g2d.getFontMetrics().stringWidth(brandText)) / 2;
        g2d.drawString(brandText, brandX, footerY + 24);
    }
}
