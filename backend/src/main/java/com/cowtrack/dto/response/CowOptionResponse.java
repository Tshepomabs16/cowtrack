package com.cowtrack.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An animal reduced to what it takes to pick it out of a list.
 *
 * <p>Exists because "let me choose an animal" and "let me browse the herd" are
 * different needs that a single endpoint serves badly. The herd list is paged,
 * which is right for a table and wrong for a dropdown — a page of it would make
 * most of the herd unselectable. Returning three columns instead of a full
 * record, with its six derived fields and their queries, is cheap enough to
 * return whole.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CowOptionResponse {
    private Long cowId;
    private String name;
    private String tagId;
}
