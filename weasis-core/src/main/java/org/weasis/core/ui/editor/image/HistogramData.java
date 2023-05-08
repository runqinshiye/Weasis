/*
 * Copyright (c) 2009-2020 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.editor.image;

import java.awt.Color;
import java.awt.image.DataBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;
import org.weasis.core.api.image.op.ByteLut;
import org.weasis.core.api.image.op.ByteLutCollection;
import org.weasis.core.api.image.op.ByteLutCollection.Lut;
import org.weasis.core.api.image.util.MeasurableLayer;
import org.weasis.core.api.image.util.WindLevelParameters;
import org.weasis.core.api.media.data.ImageElement;
import org.weasis.core.ui.Messages;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.LookupTableCV;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageConversion;
import org.weasis.opencv.op.lut.PresentationStateLut;
import org.weasis.opencv.op.lut.WlParams;

public class HistogramData {
  private final float[] histValues;
  private final MeasurableLayer layer;
  private final int bandIndex;
  private final double pixMin;
  private final double pixMax;
  private final Model colorModel;
  private WlParams windLevel;
  private DisplayByteLut lut;
  private LookupTableCV voiLut;

  public enum Model {
    GRAY(Messages.getString("HistogramData.lum"), buildLut(ByteLutCollection.Lut.GRAY)),

    RGB(
        Messages.getString("HistogramData.rgb"),
        buildLut(ByteLutCollection.Lut.RED),
        buildLut(ByteLutCollection.Lut.GREEN),
        buildLut(ByteLutCollection.Lut.BLUE)),
    HSV(
        Messages.getString("HistogramData.hsv"),
        buildLut(ByteLutCollection.Lut.HUE),
        buildLut(Messages.getString("HistogramData.saturation"), ByteLutCollection.Lut.GRAY),
        buildLut(Messages.getString("HistogramData.val"), ByteLutCollection.Lut.GRAY)),
    HLS(
        Messages.getString("HistogramData.hls"),
        buildLut(ByteLutCollection.Lut.HUE),
        buildLut(Messages.getString("HistogramData.lightness"), ByteLutCollection.Lut.GRAY),
        buildLut(Messages.getString("HistogramData.saturation"), ByteLutCollection.Lut.GRAY));

    private final ByteLut[] byteLut;
    private final String title;

    Model(String name, ByteLut... luts) {
      this.title =
          name
              + " ("
              + Arrays.stream(luts).map(ByteLut::getName).collect(Collectors.joining(","))
              + ")";
      this.byteLut = luts;
    }

    public ByteLut[] getByteLut() {
      return byteLut;
    }

    public String getTitle() {
      return title;
    }

    @Override
    public String toString() {
      return title;
    }

    private static ByteLut buildLut(Lut lut) {
      return buildLut(lut.getName(), lut);
    }

    private static ByteLut buildLut(String name, Lut lut) {
      return new ByteLut(name, lut.getByteLut().getLutTable());
    }

    private static ByteLut buildLut(String name, byte[][] slut) {
      return new ByteLut(name, slut);
    }
  }

  public HistogramData(
      float[] histValues,
      DisplayByteLut lut,
      int bandIndex,
      Model colorModel,
      WlParams windLevel,
      double pixMin,
      double pixMax,
      MeasurableLayer layer) {
    this.histValues = Objects.requireNonNull(histValues);
    this.layer = Objects.requireNonNull(layer);
    this.colorModel = colorModel;
    this.lut = lut;
    this.windLevel = windLevel;
    this.bandIndex = bandIndex;
    this.pixMin = pixMin;
    this.pixMax = pixMax;
  }

  public float[] getHistValues() {
    return histValues;
  }

  public DisplayByteLut getLut() {
    return lut;
  }

  public Model getColorModel() {
    return colorModel;
  }

  public void setLut(DisplayByteLut lut) {
    this.lut = lut;
  }

  public WlParams getWindLevel() {
    return windLevel;
  }

  public void setWindLevel(WindLevelParameters windLevel) {
    this.windLevel = windLevel;
  }

  public MeasurableLayer getLayer() {
    return layer;
  }

  public LookupTableCV getVoiLut() {
    return voiLut;
  }

  public void setVoiLut(LookupTableCV voiLut) {
    this.voiLut = voiLut;
  }

  public int getBandIndex() {
    return bandIndex;
  }

  public double getPixMin() {
    return pixMin;
  }

  public double getPixMax() {
    return pixMax;
  }

  public int getFinalVoiLutIndex(Number level) {
    if (lut == null || windLevel == null) return 0;
    Integer index = null;
    int dynamic = lut.getLutTable()[0].length - 1;

    int datatype = ImageConversion.convertToDataType(layer.getSourceRenderedImage().type());
    if (datatype >= DataBuffer.TYPE_BYTE && datatype < DataBuffer.TYPE_INT) {
      if (voiLut != null) {
        int b = voiLut.getNumBands() <= bandIndex ? 0 : bandIndex;
        index = voiLut.lookup(b, level.intValue());
      }
      PresentationStateLut pr = windLevel.getPresentationState();
      if (pr != null) {
        Optional<LookupTableCV> prLutData = pr.getPrLut();
        if (prLutData.isPresent()) {
          int val = index == null ? level.intValue() : index;
          int b = prLutData.get().getNumBands() <= bandIndex ? 0 : bandIndex;
          index = prLutData.get().lookup(b, val);
        }
      }
    }

    if (index == null) {
      double low = windLevel.getLevel() - windLevel.getWindow() / 2.0;
      double high = windLevel.getLevel() + windLevel.getWindow() / 2.0;
      double range = high - low;
      if (range < 1.0 && datatype == DataBuffer.TYPE_INT) {
        range = 1.0;
      }
      double slope = 255.0 / range;
      double yint = 255.0 - slope * high;
      index = (int) Math.round(level.doubleValue() * slope + yint);
    }
    if (index < 0) {
      index = 0;
    }
    if (index > dynamic) {
      index = dynamic;
    }

    if (lut.isInvert()) {
      index = dynamic - index;
    }
    return index;
  }

  public Color getFinalVoiLutColor(Number level) {
    if (lut == null) return Color.BLACK;
    int index = getFinalVoiLutIndex(level);
    return new Color(
        (lut.getLutTable()[2][index] & 0xff),
        (lut.getLutTable()[1][index] & 0xff),
        (lut.getLutTable()[0][index] & 0xff));
  }

  public void updateVoiLut(ViewCanvas<? extends ImageElement> view2DPane) {
    if (windLevel != null) {
      ImageElement img = view2DPane.getImage();
      PlanarImage imageSource = view2DPane.getSourceImage();
      int datatype = ImageConversion.convertToDataType(imageSource.type());
      if (datatype >= DataBuffer.TYPE_BYTE && datatype < DataBuffer.TYPE_INT) {
        Optional<LookupTableCV> prLutData = Optional.empty();
        PresentationStateLut pr = windLevel.getPresentationState();
        if (pr != null) {
          prLutData = pr.getPrLut();
        }
        if (prLutData.isEmpty() || windLevel.getLutShape().getLookup() != null) {
          setVoiLut(img.getVOILookup(windLevel));
        }
      }
    }
  }

  public static List<Mat> computeHistogram(
      Mat imageSource,
      Mat mask,
      int nbBins,
      int[] selChannels,
      Model model,
      double pixMin,
      double pixMax) {
    if (selChannels.length == 0) {
      return Collections.emptyList();
    }
    // Number of histogram bins
    MatOfInt histSize = new MatOfInt(nbBins);
    MatOfFloat histRange = new MatOfFloat((float) pixMin, (float) pixMax + 1.0f);
    Mat img;
    int cvType = CvType.depth(imageSource.type());
    if (cvType == CvType.CV_16S || cvType == CvType.CV_32S) {
      Mat floatImage = new Mat(imageSource.height(), imageSource.width(), CvType.CV_32F);
      imageSource.convertTo(floatImage, CvType.CV_32F);
      img = floatImage;
    } else {
      img = imageSource;
    }

    List<Mat> channels = new ArrayList<>();
    if (selChannels.length == 1) {
      channels.add(img);
    } else {
      if (Model.RGB == model) {
        Core.split(img, channels);
        Collections.reverse(channels);
      } else {
        ImageCV dstImg = new ImageCV();
        int code;
        if (Model.HSV == model) {
          code = Imgproc.COLOR_BGR2HSV;
        } else if (Model.HLS == model) {
          code = Imgproc.COLOR_BGR2HLS;
        } else if (Model.GRAY == model) {
          code = Imgproc.COLOR_BGR2GRAY;
        } else {
          code = Imgproc.COLOR_BGR2RGB;
        }
        Imgproc.cvtColor(img, dstImg, code);
        Core.split(dstImg, channels);
      }
    }

    Mat msk = mask == null ? new Mat() : mask;

    if (channels.size() == 1) {
      Mat hist = new Mat();
      Imgproc.calcHist(channels, new MatOfInt(0), msk, hist, histSize, histRange, false);
      return Collections.singletonList(hist);
    }

    List<Mat> histograms = new ArrayList<>();
    for (int selChannel : selChannels) {
      Mat hist = new Mat();
      Imgproc.calcHist(channels, new MatOfInt(selChannel), msk, hist, histSize, histRange, false);
      histograms.add(hist);
    }
    return histograms;
  }
}
