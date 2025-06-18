package com.example.graphqlserver;

import java.util.Arrays;
import java.util.List;

public record Author (String id, String firstName, String lastName) {
    private static List<Author> authors = Arrays.asList(
            new Author("author-1", "Dingus", "McDingus"),
            new Author("author-2", "Douglas", "Adams"),
            new Author("author-3", "Hilly", "Billy")
    );

    public static Author getById(String id) {
        return authors.stream()
				.filter(author -> author.id().equals(id))
				.findFirst()
				.orElse(null);
    }
}