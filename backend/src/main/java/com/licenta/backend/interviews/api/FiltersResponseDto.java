package com.licenta.backend.interviews.api;

import java.util.List;

public class FiltersResponseDto {
    public List<Item> levels;
    public List<Item> positions;
    public List<Item> languages;

    public static class Item {
        public Long id;
        public String name;
        public Item(Long id, String name) { this.id = id; this.name = name; }
    }
}