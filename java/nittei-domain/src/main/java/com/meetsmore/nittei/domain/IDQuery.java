package com.meetsmore.nittei.domain;

import java.util.List;

public record IDQuery(
    ID eq,
    ID ne,
    Boolean exists,
    List<ID> in,
    List<ID> nin,
    ID gt,
    ID gte,
    ID lt,
    ID lte
) {
}
