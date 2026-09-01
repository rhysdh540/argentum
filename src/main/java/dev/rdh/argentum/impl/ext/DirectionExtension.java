package dev.rdh.argentum.impl.ext;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;

public interface DirectionExtension {
	default ModelQuadFacing celeritas$toFacing() {
		throw new UnsupportedOperationException();
	}
}
