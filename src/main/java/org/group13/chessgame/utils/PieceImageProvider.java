package org.group13.chessgame.utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.group13.chessgame.model.*;

import java.util.Objects;

public class PieceImageProvider {

    public static Image getImageFor(PieceType type, PieceColor color) {
        Piece tempPiece;
        switch (type) {
            case QUEEN:
                tempPiece = new Queen(color);
                break;
            case ROOK:
                tempPiece = new Rook(color);
                break;
            case BISHOP:
                tempPiece = new Bishop(color);
                break;
            case KNIGHT:
                tempPiece = new Knight(color);
                break;
            default:
                return null;
        }
        try {
            return new Image(Objects.requireNonNull(PieceImageProvider.class.getResourceAsStream(tempPiece.getImagePath())));
        } catch (Exception e) {
            System.err.println("Error loading image for promotion: " + tempPiece.getImagePath() + " - " + e.getMessage());
            return null;
        }
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