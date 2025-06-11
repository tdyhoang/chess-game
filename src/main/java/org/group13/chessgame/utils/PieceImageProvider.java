package org.group13.chessgame.utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.group13.chessgame.model.Piece;
import org.group13.chessgame.model.PieceColor;
import org.group13.chessgame.model.PieceType;

import java.util.Objects;

public class PieceImageProvider {

    private static final Image[][] imageCache = new Image[PieceType.values().length][PieceColor.values().length];

    public static Image getImageFor(PieceType type, PieceColor color) {
        if (type == null || color == null) return null;

        int typeIndex = type.ordinal();
        int colorIndex = color.ordinal();

        if (imageCache[typeIndex][colorIndex] == null) {
            String imagePath = getImagePathFor(type, color);
            try {
                imageCache[typeIndex][colorIndex] = new Image(Objects.requireNonNull(PieceImageProvider.class.getResourceAsStream(imagePath)));
            } catch (Exception e) {
                System.err.println("Error loading image: " + imagePath + " - " + e.getMessage());
                return null;
            }
        }
        return imageCache[typeIndex][colorIndex];
    }

    private static String getImagePathFor(PieceType type, PieceColor color) {
        String colorStr = (color == PieceColor.WHITE) ? "w" : "b";
        String typeStr = Piece.pieceTypeToChar(type).toUpperCase();
        if (type == PieceType.KNIGHT) typeStr = "N";
        return String.format("/images/pieces/%s%s.png", colorStr, typeStr);
    }

    public static ImageView getImageViewFor(PieceType type, PieceColor color, double size) {
        Image img = getImageFor(type, color);
        if (img != null) {
            ImageView imgView = new ImageView(img);
            imgView.setFitWidth(size);
            imgView.setFitHeight(size);
            imgView.setPreserveRatio(true);
            return imgView;
        }
        return new ImageView();
    }
}