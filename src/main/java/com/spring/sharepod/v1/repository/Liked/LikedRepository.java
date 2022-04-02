package com.spring.sharepod.v1.repository.Liked;

import com.spring.sharepod.entity.Board;
import com.spring.sharepod.entity.Liked;
import com.spring.sharepod.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface LikedRepository extends JpaRepository<Liked, Long>, LikedRepositoryCustom {

    //16번 찜하기, 찜하기 취소
    @Query("select c from Liked c where c.user=:user and c.board=:board")
    Liked findByUserAndBoard(User user, Board board);

    Boolean existsByUserIdAndBoardId(Long usrId,Long BoardId);
}
