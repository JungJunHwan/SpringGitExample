package com.example.board.member.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.example.board.member.model.Member;

@Repository
@Mapper
public interface IMemberRepository {
	void insertMember(Member member) ;
	Member selectMember(String userid);
	List<Member> selectAllMembers();
	void updateMember(Member member);
	void deleteMember(Member member);
	String getPassword(String userid);
}