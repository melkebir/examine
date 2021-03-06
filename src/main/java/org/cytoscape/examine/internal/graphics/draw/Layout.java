package org.cytoscape.examine.internal.graphics.draw;

import org.cytoscape.examine.internal.graphics.AnimatedGraphics;
import org.cytoscape.examine.internal.graphics.PVector;

import java.util.Iterator;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Utility class for laying out PositionedSnippets (in two dimensions).
 */
public class Layout {
    
    /**
     * Place the given snippet at the given position.
     */
    public static void place(PositionedSnippet snippet, PVector position) {
        snippet.topLeft.set(position);
    }
    
    /**
     * Place the second given snippet left of the first given snippet,
     * with the given space in between. Aligned to top.
     */
    public static void placeLeftTop(
            AnimatedGraphics graphics,
            PositionedSnippet placedSnippet,
            PositionedSnippet toPlaceSnippet,
            double space) {
        // To left.
        toPlaceSnippet.topLeft.x =
                placedSnippet.topLeft.x + placedSnippet.dimensions(graphics).x + space;
        
        // Top aligned.
        toPlaceSnippet.topLeft.y = placedSnippet.topLeft.y;
    }
    
    /**
     * Place the given snippets in a left to right row, with the given
     * space between snippets, starting at the given position.
     * Returns the bounds of all snippets.
     */
    public static <E extends PositionedSnippet> void placeLeftTop(
            AnimatedGraphics graphics,
            PVector topLeft,
            Iterable<E> snippets,
            double space) {
        // Place first snippet at topLeft.
        Iterator<E> sIt = snippets.iterator();
        if(sIt.hasNext()) {
            PositionedSnippet s = sIt.next();
            place(s, topLeft);

            // Place remainder as a row.
            while(sIt.hasNext()) {
                PositionedSnippet toPlace = sIt.next();
                placeLeftTop(graphics, s, toPlace, space);
                s = toPlace;
            }
        }
    }
    
    /**
     * Place the second given snippet below the first given snippet,
     * with the given space in between. Aligned to the left.
     */
    public static void placeBelowLeft(
            AnimatedGraphics graphics,
            PositionedSnippet placedSnippet,
            PositionedSnippet toPlaceSnippet,
            double space) {
        // Below.
        toPlaceSnippet.topLeft.y =
                placedSnippet.topLeft.y + placedSnippet.dimensions(graphics).y + space;
        
        // Left aligned.
        toPlaceSnippet.topLeft.x = placedSnippet.topLeft.x;
    }
    
    /**
     * Place the given snippets in a top to bottom column, with the given
     * space between snippets, starting at the given position.
     * Returns the bounds of all snippets.
     */
    public static <E extends PositionedSnippet> void placeBelowLeft(
            AnimatedGraphics graphics,
            PVector topLeft,
            Iterable<E> snippets,
            double space) {
        // Place first snippet at topLeft.
        Iterator<E> sIt = snippets.iterator();
        if(sIt.hasNext()) {
            PositionedSnippet s = sIt.next();
            place(s, topLeft);

            // Place remainder as a column.
            while(sIt.hasNext()) {
                PositionedSnippet toPlace = sIt.next();
                placeBelowLeft(graphics, s, toPlace, space);
                s = toPlace;
            }
        }
    }
    
    public static <E extends PositionedSnippet> void placeBelowLeftToRight(
            AnimatedGraphics graphics,
            PVector topLeft,
            Iterable<E> snippets,
            double space,
            double verticalBound) {
        // Place first snippet at topLeft.
        Iterator<E> sIt = snippets.iterator();
        if(sIt.hasNext()) {
            PositionedSnippet s = sIt.next();
            place(s, topLeft);
            
            double maxWidth = s.dimensions(graphics).x;

            // Place remainder as a column, switch to next column when
            // vertical space limit has been reached.
            while(sIt.hasNext()) {
                PositionedSnippet toPlace = sIt.next();
                
                // Next column.
                if(s.topLeft.y + s.dimensions(graphics).y +
                   space + toPlace.dimensions(graphics).y > verticalBound) {
                    place(toPlace, PVector.v(maxWidth + space, topLeft.y));
                }
                // Next row.
                else {
                    placeBelowLeft(graphics, s, toPlace, space);
                }
                s = toPlace;
                
                maxWidth = max(maxWidth, toPlace.topLeft.x + toPlace.dimensions(graphics).x);
            }
        }
    }
    
    /**
     * Get the union bounds of the given snippets.
     */
    public static <E extends PositionedSnippet> PVector bounds(AnimatedGraphics graphics, Iterable<E> snippets) {
        PVector bounds;
        
        // Snippets are present.
        if(snippets.iterator().hasNext()) {
            double minX = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            
            for(E s: snippets) {
                PVector sD = s.dimensions(graphics);
                minX = min(minX, s.topLeft.x);
                maxX = max(maxX, s.topLeft.x + sD.x);
                minY = min(minY, s.topLeft.y);
                maxY = max(maxY, s.topLeft.y + sD.y);
            }
            
            bounds = PVector.v(maxX - minX, maxY - minY);
        }
        // No snippets.
        else {
            bounds = PVector.v();
        }
        
        return bounds;
    }
    
    /**
     * Get the union bounds of the given snippets, increased by
     * the given space if there are any snippets.
     */
    public static <E extends PositionedSnippet> PVector spacedBounds(
            AnimatedGraphics graphics,
            Iterable<E> snippets,
            double space) {
        PVector bounds = bounds(graphics, snippets);
        
        if(snippets.iterator().hasNext()) {
            bounds = PVector.add(bounds, new PVector(space, space));
        }
        
        return bounds;
    }
    
    /**
     * Maximum width of given snippets.
     */
    public static <E extends PositionedSnippet> double maxWidth(AnimatedGraphics graphics, Iterable<E> snippets) {
        double maxWidth = 0;
        
        for(E s: snippets) {
            maxWidth = max(maxWidth, s.dimensions(graphics).x);
        }
        
        return maxWidth;
    }
    
    /**
     * Maximum height of given snippets.
     */
    public static<E extends PositionedSnippet> double maxHeight(AnimatedGraphics graphics, Iterable<E> snippets) {
        double maxHeight = 0;
        
        for(E s: snippets) {
            maxHeight = max(maxHeight, s.dimensions(graphics).y);
        }
        
        return maxHeight;
    }
    
}
