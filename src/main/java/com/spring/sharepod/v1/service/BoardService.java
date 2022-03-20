package com.spring.sharepod.v1.service;

import com.spring.sharepod.entity.Amount;
import com.spring.sharepod.entity.Board;
import com.spring.sharepod.entity.ImgFiles;
import com.spring.sharepod.entity.User;
import com.spring.sharepod.exception.CommonError.ErrorCode;
import com.spring.sharepod.exception.CommonError.ErrorCodeException;
import com.spring.sharepod.v1.dto.request.BoardRequestDto;
import com.spring.sharepod.v1.dto.response.BasicResponseDTO;
import com.spring.sharepod.v1.dto.response.BoardResponseDto;
import com.spring.sharepod.v1.repository.AmountRepository;
import com.spring.sharepod.v1.repository.BoardRepository;
import com.spring.sharepod.v1.repository.ImgFilesRepository;
import com.spring.sharepod.v1.repository.UserRepository;
import com.spring.sharepod.v1.validator.BoardValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.spring.sharepod.exception.CommonError.ErrorCode.BOARD_NOT_EQUAL_WRITER;
import static com.spring.sharepod.exception.CommonError.ErrorCode.BOARD_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    private final AwsS3Service awsS3Service;

    private final BoardValidator boardValidator;
    private final ImgFilesRepository imgFilesRepository;

    private final AmountRepository amountRepository;


    //8번 API 릴스 video 전체 GET(Limit) (구현 완료)
    @Transactional
    public List<BoardResponseDto.VideoAll> getAllVideo(Long limitCount) {
        // 모든 릴스 가져오기
        List<Board> boardList = boardRepository.findAllByVideoUrlRan(limitCount);

        // 릴스를 반환해서 저장할 리스트
        List<BoardResponseDto.VideoAll> videoAllResponseDtos = new ArrayList<>();

        // 릴스 덩어리 해쳐서 넣어주기
        for (Board board : boardList) {
            String nickname = board.getUser().getNickName();
            String userimg = board.getUser().getUserImg();

            // videoAllResponseDto 생성
            BoardResponseDto.VideoAll videoAllResponseDto = BoardResponseDto.VideoAll.builder()
                    .boardId(board.getId())
                    .videoUrl(board.getImgFiles().getVideoUrl())
                    .boardTag(board.getBoardTag())
                    .Title(board.getTitle())
                    .userImg(userimg)
                    .nickName(nickname)
                    .build();

            // 반환할 리스트에 저장하기
            videoAllResponseDtos.add(videoAllResponseDto);
        }

        return videoAllResponseDtos;
    }

    //9번 API 게시판 작성 (구현 완료)
    @Transactional
    public BasicResponseDTO wirteBoard(BoardRequestDto.WriteBoard writeBoard) {

        //작성자의 id로 user를 찾는다.
        User user = userRepository.findById(writeBoard.getUserId()).orElseThrow(
                () -> new ErrorCodeException(ErrorCode.USER_NOT_FOUND));


        //보드 저장
        Long boardId = boardRepository.save(Board.builder()
                .user(user)
                .title(writeBoard.getTitle())
                .contents(writeBoard.getContents())
                .boardRegion(writeBoard.getBoardRegion())
                .category(writeBoard.getCategory())
                .productQuality(writeBoard.getProductQuality())
                .boardTag(writeBoard.getBoardTag())
                .mainAppear(true)
                .build()).getId();

        //보드를 저장한 Id를 통해 imgfiles와 amount를 저장한다.
        Board board = boardRepository.findById(boardId).orElseThrow(() -> new ErrorCodeException(BOARD_NOT_FOUND));

        imgFilesRepository.save(ImgFiles.builder()
                .firstImgUrl(writeBoard.getFirstImgUrl())
                .secondImgUrl(writeBoard.getSecondImgUrl())
                .lastImgUrl(writeBoard.getLastImgUrl())
                .videoUrl(writeBoard.getVideoUrl())
                .board(board)
                .build());

        amountRepository.save(Amount.builder()
                .dailyRentalFee(writeBoard.getDailyRentalFee())
                .originPrice(writeBoard.getOriginPrice())
                .board(board)
                .build());

        return BasicResponseDTO.builder()
                .result("success")
                .msg(user.getNickName() + "님의 게시글 작성 완료되었습니다.")
                .build();
    }

    // 10번 API 상세 페이지 board GET (구현 완료)
    @Transactional
    public BoardResponseDto.BoardDetail getDetailBoard(Long boardId, Boolean isLiked) {
        //보드가 존재하지 않을 시 메시지 호출
        Board board = boardValidator.ValidByBoardId(boardId);
        String[] imgs = {board.getImgFiles().getFirstImgUrl(),board.getImgFiles().getSecondImgUrl(),board.getImgFiles().getLastImgUrl()};
        List<String> fileName = Arrays.asList(imgs);

        // 존재한다면 받아온 내용들을 담아서 보내주기
        BoardResponseDto.BoardDetail boardDetailResponseDto = BoardResponseDto.BoardDetail.builder()
                .Title(board.getTitle())
                .videoUrl(board.getImgFiles().getVideoUrl())
                .imgFiles(fileName)
//                .secondImgUrl(board.getImgFiles().getSecondImgUrl())
//                .lastImgUrl(board.getImgFiles().getLastImgUrl())
                .contents(board.getContents())
                .originPrice(board.getAmount().getOriginPrice())
                .dailyRentalFee(board.getAmount().getDailyRentalFee())
                .boardTag(board.getBoardTag())
                .nickName(board.getUser().getNickName())
                .sellerRegion(board.getUser().getUserRegion())
                .boardRegion(board.getBoardRegion())
                .category(board.getCategory())
                .boardQuaility(board.getProductQuality())
                .isLiked(isLiked)
                .likeCount(board.getLikeNumber().size())
                .sellerImg(board.getUser().getUserImg())
                .modifiedAt(String.valueOf(board.getModifiedAt()))
                .build();

        return boardDetailResponseDto;
    }


    //11번 API 게시판 수정 (구현 완료)
    @Transactional
    public BasicResponseDTO updateBoard(Long boardId, BoardRequestDto.PatchBoard patchRequestDTO) {

        //수정할 게시판 boardid로 검색해 가져오기
        Board board = boardRepository.findById(boardId).orElseThrow(
                () -> new ErrorCodeException(ErrorCode.BOARD_NOT_FOUND)
        );

        //받아온 userid와 boardid의 작성자가 다를때
        if (!Objects.equals(patchRequestDTO.getUserId(), board.getUser().getId())) {
            throw new ErrorCodeException(BOARD_NOT_EQUAL_WRITER);
        }

        //게시판 업데이트
        board.getImgFiles().updateImgFiles(patchRequestDTO.getFirstImgUrl(), patchRequestDTO.getSecondImgUrl(), patchRequestDTO.getLastImgUrl(), patchRequestDTO.getVideoUrl());
        board.getAmount().updateAmount(patchRequestDTO.getOriginPrice(), patchRequestDTO.getDailyRentalFee());
        board.updateBoard(patchRequestDTO.getTitle(), patchRequestDTO.getContents(), patchRequestDTO.getCategory(), patchRequestDTO.getBoardRegion(), patchRequestDTO.getProductQuality(), patchRequestDTO.getBoardTag());

        return BasicResponseDTO.builder()
                .result("success")
                .msg(board.getUser().getNickName() + "님의 게시글 수정 완료되었습니다.")
                .build();
    }


    //12번 API 게시판 삭제 (구현 완료)
    @Transactional
    public BasicResponseDTO deleteboard(Long boardId, Long userId) {

        //삭제할 게시판 boardid로 검색해 가져오기
        Board board = boardRepository.findById(boardId).orElseThrow(
                () -> new ErrorCodeException(BOARD_NOT_FOUND)
        );

        //받아온 userid와 boardid의 작성자가 다를때
        if (!Objects.equals(userId, board.getUser().getId())) {
            throw new ErrorCodeException(BOARD_NOT_EQUAL_WRITER);
        }

        //DB에 존재하는 풀 길이의 Url을 받아와서 제거하기 위한 키를 만들어준다.
        String firstImg = board.getImgFiles().getFirstImgUrl();
        String secondImg = board.getImgFiles().getSecondImgUrl();
        String lastImg = board.getImgFiles().getLastImgUrl();
        String videoUrl = board.getImgFiles().getVideoUrl();

        firstImg = firstImg.substring(firstImg.lastIndexOf("/") + 1);
        secondImg = secondImg.substring(secondImg.lastIndexOf("/") + 1);
        lastImg = lastImg.substring(lastImg.lastIndexOf("/") + 1);
        videoUrl = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);

        //리스트에 담에서 넣어주기
        String[] imgs = {firstImg, secondImg, lastImg, videoUrl};
        List<String> fileName = Arrays.asList(imgs);


        awsS3Service.deleteBoardFiles(fileName);


        //게시글 삭제
        boardRepository.deleteById(boardId);

        return BasicResponseDTO.builder()
                .result("success")
                .msg(board.getUser().getNickName() + "번 게시글 삭제 완료되었습니다.")
                .build();
    }


    //API 13번 메인 페이지 전체 게시글 불러오기 (구현 완료)
    @Transactional
    public List<BoardResponseDto.BoardAll> getAllBoard(Long limitCount) {

        // 모든 게시글 가져오기
        List<Board> boardList = boardRepository.findAllByOrderByModifiedAtDesc(limitCount);
        return getBoardService(boardList);
    }


    //15번 카테고리 정렬별 보여주기 (구현 완료)
    @Transactional
    public List<BoardResponseDto.BoardAll> getSortedBoard(String filterType, String category, String boardRegion, Long limitCount) {
        List<Board> boardList = new ArrayList<>();
        System.out.println("filterType : " + filterType);
        System.out.println("category : " + category);
        System.out.println("boardRegion :" + boardRegion);
        System.out.println("limitCount" + limitCount);
        switch (filterType) {
            case "quality":
                boardList = boardRepository.findByAndMapAndCategoryByQuility(boardRegion, category, limitCount);
                break;

            case "cost":
                boardList = boardRepository.findByAndMapAndCategoryByCost(boardRegion, category, limitCount);
                break;

            default:
                boardList = boardRepository.findByAndMapAndCategoryByCreatedAt(boardRegion, category, limitCount);

        }

        return getBoardService(boardList);
    }


    //15번 검색한 내용에 대한 정보 (구현 완료)
    @Transactional
    public List<BoardResponseDto.BoardAll> getSearchBoard(String filtertype, String searchtitle, String mapdata, Long limitcount) {
        List<Board> boardList = new ArrayList<>();

        switch (filtertype) {
            case "quality":
                boardList = boardRepository.findByAndMapAndSearchByQuility(mapdata, searchtitle, limitcount);
                break;

            case "cost":
                boardList = boardRepository.findByAndMapAndSearchByCost(mapdata, searchtitle, limitcount);
                break;

            default:
                boardList = boardRepository.findByAndMapAndSearchByCreatedAt(mapdata, searchtitle, limitcount);

        }
        return getBoardService(boardList);
    }


    //////////////////반복되는 로직을 처리해주는 함수들
    public List<BoardResponseDto.BoardAll> getBoardService(List<Board> boardList) {
        // 게시글을 반환해서 저장할 리스트
        List<BoardResponseDto.BoardAll> boardResponseDtos = new ArrayList<>();

        // 게시글 해쳐서 for문을 통해 하나씩 넣어주기
        for (Board board : boardList) {
            // BoardResponseDto 생성
            BoardResponseDto.BoardAll boardResponseDto = BoardResponseDto.BoardAll.builder()
                    .boardId(board.getId())
                    .category(board.getCategory())
                    .Title(board.getTitle())
                    .firstImgUrl(board.getImgFiles().getFirstImgUrl())
                    .dailyRentalFee(board.getAmount().getDailyRentalFee())
                    .boardContents(board.getContents())
                    .boardTag(board.getBoardTag())
                    .sellerImgUrl(board.getUser().getUserImg())
                    .sellerNickName(board.getUser().getNickName())
                    .build();

            // 반환할 리스트에 저장하기
            boardResponseDtos.add(boardResponseDto);
        }

        return boardResponseDtos;
    }
}
