package view;

import java.awt.geom.Path2D;

import model.TrackGeometry;

/**
 * Builds the track outline by drawing each tile's centerline geometry.
 *
 * <p>Delegates to {@link TrackGeometry} for the shared nominal path definition.
 */
final class TrackPreviewPathBuilder {

	private TrackPreviewPathBuilder() {
	}

	static Path2D buildTrackPath(int[][] trackMap) {
		return TrackGeometry.buildPreviewPath(trackMap);
	}
}
