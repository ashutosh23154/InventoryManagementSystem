package Helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TrieNode implements Serializable {
    Map<Character, TrieNode> children;
    boolean isEndOfWord;

    public TrieNode() {
        children = new HashMap<>();
        isEndOfWord = false;
    }
}

public class Trie implements Serializable{
    private final TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    public void insert(String word) {
        if (word == null) return;
        word = word.toLowerCase();
        TrieNode node = root;
        for (char ch : word.toCharArray()) {
            node = node.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        node.isEndOfWord = true;
    }

    public List<String> startsWith(String prefix) {
        List<String> results = new ArrayList<>();
        if (prefix == null) return results;
        prefix = prefix.toLowerCase();
        TrieNode node = root;

        for (char ch : prefix.toCharArray()) {
            node = node.children.get(ch);
            if (node == null) {
                return results;
            }
        }

        collectAllWords(node, new StringBuilder(prefix), results);
        return results;
    }

    private void collectAllWords(TrieNode node, StringBuilder prefix, List<String> results) {
        if (node.isEndOfWord) {
            results.add(prefix.toString());
        }

        for (Map.Entry<Character, TrieNode> entry : node.children.entrySet()) {
            prefix.append(entry.getKey());
            collectAllWords(entry.getValue(), prefix, results);
            prefix.deleteCharAt(prefix.length() - 1);
        }
    }

    public boolean delete(String word) {
        return deleteHelper(root, word.toLowerCase(), 0);
    }

    private boolean deleteHelper(TrieNode node, String word, int depth) {
        if (node == null) {
            return false;
        }

        if (depth == word.length()) {
            if (node.isEndOfWord) {
                node.isEndOfWord = false;
                return node.children.isEmpty();
            }
            return false;
        }

        char ch = word.charAt(depth);
        boolean shouldDeleteCurrentNode = deleteHelper(node.children.get(ch), word, depth + 1);

        if (shouldDeleteCurrentNode) {
            node.children.remove(ch);
            return node.children.isEmpty() && !node.isEndOfWord;
        }

        return false;
    }
}
