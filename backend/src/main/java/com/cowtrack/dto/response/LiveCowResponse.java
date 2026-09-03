package com.cowtrack.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One animal as it appears on the live map: where it is, and enough about it to
 * draw and label the marker.
 *
 * <p>Distinct from {@link CowResponse} because the map needs a little about every
 * animal, where the herd list needs everything about a few. Reusing the full
 * record here meant loading breed, weight and vital signs for the entire herd to
 * decide what colour to draw a dot, and each of those fields costs its own query
 * per animal.
 *
 * <p>The rest of an animal's detail is fetched when its marker is actually
 * clicked, which is the only time anyone looks at it.
 */
@Data
public class LiveCowResponse {

    private Long locationId;
    private Long cowId;
    private String cowName;
    private String tagId;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordedAt;

    /** {@code alert}, {@code inactive} or {@code healthy}. */
    private String status;
}
