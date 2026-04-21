package com.meetsmore.nittei.domain;

import java.util.List;

public record StringQuery(String eq, String ne, Boolean exists, List<String> in) {}
