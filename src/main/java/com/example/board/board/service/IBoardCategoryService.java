package com.example.board.board.service;

import java.util.List;

import com.example.board.board.model.BoardCategory;

public interface IBoardCategoryService {
	List<BoardCategory> selectAllCategory();
	void insertNewCategory(BoardCategory boardCategory);
	void updateCategory(BoardCategory boardCategory);
	void deleteCategory(int categoryId);
}