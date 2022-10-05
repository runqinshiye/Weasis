/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.codec.geometry;

import java.awt.geom.Point2D;
import java.util.List;
import org.joml.Vector3d;

/**
 * @author David A. Clunie
 */
public class IntersectSlice extends LocalizerPoster {

  public IntersectSlice(
      Vector3d row, Vector3d column, Vector3d tlhc, Vector3d voxelSpacing, Vector3d dimensions) {
    super(row, column, tlhc, voxelSpacing, dimensions);
  }

  public IntersectSlice(GeometryOfSlice geometry) {
    super(geometry);
  }

  private static boolean allTrue(boolean[] array) {
    boolean all = true;
    for (boolean b : array) {
      if (!b) {
        all = false;
        break;
      }
    }
    return all;
  }

  private static boolean oppositeEdges(boolean[] array) {
    return array[0] && array[2] || array[1] && array[3];
  }

  private static boolean adjacentEdges(boolean[] array) {
    return array[0] && array[1]
        || array[1] && array[2]
        || array[2] && array[3]
        || array[3] && array[0];
  }

  private static boolean[] classifyCornersOfRectangleIntoEdgesCrossingZPlane(Vector3d[] corners) {
    int size = corners.length;
    boolean[] classification = new boolean[size];
    for (int i = 0; i < size; ++i) {
      int next = (i == size - 1) ? 0 : i + 1;
      classification[i] = classifyCornersIntoEdgeCrossingZPlane(corners[i], corners[next]);
    }
    return classification;
  }

  @Override
  public List<Point2D> getOutlineOnLocalizerForThisGeometry(
      Vector3d row,
      Vector3d column,
      Vector3d tlhc,
      Vector3d voxelSpacing,
      double sliceThickness,
      Vector3d dimensions) {

    Vector3d[] corners =
        getCornersOfSourceRectangleInSourceSpace(row, column, tlhc, voxelSpacing, dimensions);
    for (int i = 0; i < 4; ++i) {
      // We want to consider each edge of the source slice with respect to
      // the plane of the target localizer, so transform the source corners
      // into the target localizer space, and then see which edges cross
      // the Z plane of the localizer

      corners[i] = transformPointFromSourceSpaceIntoLocalizerSpace(corners[i]);

      // Now, points with a Z value of zero are in the plane of the localizer plane
      // Edges with one Z value +ve (or 0) and the other -ve (or 0) cross (or touch) the localizer
      // plane
      // Edges with both Z values +ve or both -ve don't cross the localizer plane
    }

    boolean[] edges = classifyCornersOfRectangleIntoEdgesCrossingZPlane(corners);

    if (allTrue(edges)) {
      // "Source in exactly the same plane as the localizer
      return drawOutlineOnLocalizer(corners); // draw a rectangle
    } else if (oppositeEdges(edges)) {
      // draw line between where two edges cross (have zero Z value)
      return drawLinesBetweenAnyPointsWhichIntersectPlaneWhereZIsZero(corners);
    } else if (adjacentEdges(edges)) {
      // draw line between where two edges cross (have zero Z value)
      return drawLinesBetweenAnyPointsWhichIntersectPlaneWhereZIsZero(corners);
    } else {
      // No edges cross the localizer
      return null;
    }
  }
}
