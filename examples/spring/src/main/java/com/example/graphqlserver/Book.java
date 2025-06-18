package com.example.graphqlserver;

import java.util.Arrays;
import java.util.List;

public record Book (String id, String name, int pageCount, String authorId) {
    private static List<Book> books = Arrays.asList(
            new Book("book-1", "Why Go is better", 416, "author-1"),
            new Book("book-2", "Hitchhiker's Guide to the Galaxy", 208, "author-2"),
            new Book("book-3", "Down Under JVM", 436, "author-3")
    );

    public static Book getById(String id) {
        return books.stream()
				.filter(book -> book.id().equals(id))
				.findFirst()
				.orElse(null);
    }
}
