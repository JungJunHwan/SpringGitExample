package com.example.board.board.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.board.board.model.Board;
import com.example.board.board.model.BoardCategory;
import com.example.board.board.model.BoardUploadFile;
import com.example.board.board.service.IBoardCategoryService;
import com.example.board.board.service.IBoardService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class BoardController {
    static final Logger logger = LoggerFactory.getLogger(BoardController.class);

    @Autowired
    IBoardService boardService;

    @Autowired
    IBoardCategoryService categoryService;

    // 1. 카테고리별 목록 조회 (페이징 포함)
    @GetMapping("/board/cat/{categoryId}/{page}")
    public String getListByCategory(@PathVariable int categoryId, @PathVariable int page, HttpSession session, Model model) {
        session.setAttribute("page", page);
        model.addAttribute("categoryId", categoryId);

        List<Board> boardList = boardService.selectArticleListByCategory(categoryId, page);
        model.addAttribute("boardList", boardList);

        int bbsCount = boardService.selectTotalArticleCountByCategoryId(categoryId);
        int totalPage = 0;
        if(bbsCount > 0) {
            totalPage = (int)Math.ceil(bbsCount/10.0);
        }
        int totalPageBlock = (int)(Math.ceil(totalPage/10.0));
        int nowPageBlock = (int)Math.ceil(page/10.0);
        int startPage = (nowPageBlock-1)*10 + 1;
        int endPage = 0;
        if(totalPage > nowPageBlock*10) {
            endPage = nowPageBlock*10;
        } else {
            endPage = totalPage;
        }
        model.addAttribute("totalPageCount", totalPage);
        model.addAttribute("nowPage", page);
        model.addAttribute("totalPageBlock", totalPageBlock);
        model.addAttribute("nowPageBlock", nowPageBlock);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        return "board/list";
    }

    // 1-1. 카테고리별 목록 조회 (페이지 미지정 시 1페이지로)
    @GetMapping("/board/cat/{categoryId}")
    public String getListByCategory(@PathVariable int categoryId, HttpSession session, Model model) {
        return getListByCategory(categoryId, 1, session, model);
    }

    // 2. 게시글 상세 보기
    @GetMapping("/board/{boardId}/{page}")
    public String getBoardDetails(@PathVariable int boardId, @PathVariable int page, Model model) {
        Board board = boardService.selectArticle(boardId);
        String fileName = board.getFileName();
        if(fileName!=null) {
            int fileLength = fileName.length();
            String fileType = fileName.substring(fileLength-4, fileLength).toUpperCase();
            model.addAttribute("fileType", fileType);
        }
        model.addAttribute("board", board);
        model.addAttribute("page", page);
        model.addAttribute("categoryId", board.getCategoryId());
        logger.info("getBoardDetails " + board.toString());
        return "board/view";
    }

    // 2-1. 상세 보기 (페이지 미지정 시 1페이지로)
    @GetMapping("/board/{boardId}")
    public String getBoardDetails(@PathVariable int boardId, Model model) {
        return getBoardDetails(boardId, 1, model);
    }

    // 3. 글쓰기 폼 이동
    @GetMapping(value="/board/write/{categoryId}")
    public String writeArticle(@PathVariable int categoryId, Model model) {
        List<BoardCategory> categoryList = categoryService.selectAllCategory();
        model.addAttribute("categoryList", categoryList);
        model.addAttribute("categoryId", categoryId);
        return "board/write";
    }

    // 4. 글 등록 처리 (파일 업로드 포함)
    @PostMapping(value="/board/write")
    public String writeArticle(Board board, BindingResult results, RedirectAttributes redirectAttrs) {
        logger.info("/board/write : " + board.toString());
        try {
            board.setContent(board.getContent().replace("\r\n", "<br>"));
            board.setTitle(Jsoup.clean(board.getTitle(), Safelist.basic()));
            board.setContent(Jsoup.clean(board.getContent(), Safelist.basic()));
            MultipartFile mfile = board.getFile();
            if(mfile!=null && !mfile.isEmpty()) {
                BoardUploadFile file = new BoardUploadFile();
                file.setFileName(mfile.getOriginalFilename());
                file.setFileSize(mfile.getSize());
                file.setFileContentType(mfile.getContentType());
                file.setFileData(mfile.getBytes());
                boardService.insertArticle(board, file);
            } else {
                boardService.insertArticle(board);
            }
        } catch(Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/board/cat/"+board.getCategoryId();
    }

    // 5. 파일 다운로드/보기
    @GetMapping("/file/{fileId}")
    public ResponseEntity<byte[]> getFile(@PathVariable int fileId) {
        BoardUploadFile file = boardService.getFile(fileId);
        logger.info("getFile " + file.toString());
        final HttpHeaders headers = new HttpHeaders();
        String[] mtypes = file.getFileContentType().split("/");
        headers.setContentType(new MediaType(mtypes[0], mtypes[1]));
        headers.setContentLength(file.getFileSize());
        try {
            String encodedFileName = URLEncoder.encode(file.getFileName(), "UTF-8");
            headers.setContentDispositionFormData("attachment", encodedFileName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return new ResponseEntity<byte[]>(file.getFileData(), headers, HttpStatus.OK);
    }

    // 6. 답글 폼 이동
    @GetMapping(value="/board/reply/{boardId}")
    public String replyArticle(@PathVariable int boardId, Model model) {
        Board board = boardService.selectArticle(boardId);
        board.setWriter("");
        board.setEmail("");
        board.setTitle("[Re]"+board.getTitle());
        board.setContent("\n\n\n----------\n" + board.getContent().replaceAll("<br>", "\n"));
        model.addAttribute("board", board);
        model.addAttribute("next", "reply");
        return "board/reply";
    }

    // 7. 답글 등록 처리
    @PostMapping(value="/board/reply")
    public String replyArticle(Board board, RedirectAttributes redirectAttrs, HttpSession session) {
        logger.info("/board/reply : " + board.toString());
        try {
            board.setContent(board.getContent().replace("\r\n", "<br>"));
            board.setTitle(Jsoup.clean(board.getTitle(), Safelist.basic()));
            board.setContent(Jsoup.clean(board.getContent(), Safelist.basic()));
            MultipartFile mfile = board.getFile();
            if(mfile!=null && !mfile.isEmpty()) {
                BoardUploadFile file = new BoardUploadFile();
                file.setFileName(mfile.getOriginalFilename());
                file.setFileSize(mfile.getSize());
                file.setFileContentType(mfile.getContentType());
                file.setFileData(mfile.getBytes());
                boardService.replyArticle(board, file);
            } else {
                boardService.replyArticle(board);
            }
        } catch(Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("message", e.getMessage());
        }
        if(session.getAttribute("page") != null) {
            return "redirect:/board/cat/"+board.getCategoryId() + "/" + (Integer)session.getAttribute("page");
        } else {
            return "redirect:/board/cat/"+board.getCategoryId();
        }
    }

    // 8. 수정 폼 이동
    @GetMapping(value="/board/update/{boardId}")
    public String updateArticle(@PathVariable int boardId, Model model) {
        List<BoardCategory> categoryList = categoryService.selectAllCategory();
        Board board = boardService.selectArticle(boardId);
        model.addAttribute("categoryList", categoryList);
        model.addAttribute("categoryId", board.getCategoryId());
        board.setContent(board.getContent().replaceAll("<br>", "\r\n"));
        model.addAttribute("board", board);
        return "board/update";
    }

    // 9. 수정 처리
    @PostMapping(value="/board/update")
    public String updateArticle(Board board, RedirectAttributes redirectAttrs) {
        logger.info("/board/update " + board.toString());
        String dbPassword = boardService.getPassword(board.getBoardId());
        if(!board.getPassword().equals(dbPassword)) {
            redirectAttrs.addFlashAttribute("passwordError", "게시글 비밀번호가 다릅니다");
            return "redirect:/board/update/" + board.getBoardId();
        }
        try {
            board.setContent(board.getContent().replace("\r\n", "<br>"));
            board.setTitle(Jsoup.clean(board.getTitle(), Safelist.basic()));
            board.setContent(Jsoup.clean(board.getContent(), Safelist.basic()));
            MultipartFile mfile = board.getFile();
            if(mfile!=null && !mfile.isEmpty()) {
                logger.info("/board/update : " + mfile.getOriginalFilename());
                BoardUploadFile file = new BoardUploadFile();
                file.setFileId(board.getFileId());
                file.setFileName(mfile.getOriginalFilename());
                file.setFileSize(mfile.getSize());
                file.setFileContentType(mfile.getContentType());
                file.setFileData(mfile.getBytes());
                logger.info("/board/update : " + file.toString());
                boardService.updateArticle(board, file);
            } else {
                boardService.updateArticle(board);
            }
        } catch(Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/board/"+board.getBoardId();
    }

    // 10. 삭제 폼 이동
    @GetMapping(value="/board/delete/{boardId}")
    public String deleteArticle(@PathVariable int boardId, Model model) {
        Board board = boardService.selectDeleteArticle(boardId);
        model.addAttribute("categoryId", board.getCategoryId());
        model.addAttribute("boardId", boardId);
        model.addAttribute("replyNumber", board.getReplyNumber());
        return "board/delete";
    }

    // 11. 삭제 처리
    @PostMapping(value="/board/delete")
    public String deleteArticle(Board board, HttpSession session, RedirectAttributes model) {
        try {
            String dbpw = boardService.getPassword(board.getBoardId());
            if(dbpw.equals(board.getPassword())) {
                boardService.deleteArticle(board.getBoardId(), board.getReplyNumber());
                return "redirect:/board/cat/" + board.getCategoryId() + "/" + (Integer)session.getAttribute("page");
            } else {
                model.addFlashAttribute("message", "WRONG_PASSWORD_NOT_DELETED");
                return "redirect:/board/delete/" + board.getBoardId();
            }
        } catch(Exception e) {
            model.addAttribute("message", e.getMessage());
            e.printStackTrace();
            return "error/runtime";
        }
    }

    // 12. 검색 처리
    @GetMapping("/board/search/{page}")
    public String search(@RequestParam(required=false, defaultValue="") String keyword, @PathVariable int page, HttpSession session, Model model) {
        try {
            List<Board> boardList = boardService.searchListByContentKeyword(keyword, page);
            model.addAttribute("boardList", boardList);
            int bbsCount = boardService.selectTotalArticleCountByKeyword(keyword);
            int totalPage = 0;
            if(bbsCount > 0) {
                totalPage = (int)Math.ceil(bbsCount/10.0);
            }
            int totalPageBlock = (int)(Math.ceil(totalPage/10.0));
            int nowPageBlock = (int)Math.ceil(page/10.0);
            int startPage = (nowPageBlock-1)*10 + 1;
            int endPage = 0;
            if(totalPage > nowPageBlock*10) {
                endPage = nowPageBlock*10;
            } else {
                endPage = totalPage;
            }
            model.addAttribute("keyword", keyword);
            model.addAttribute("totalPageCount", totalPage);
            model.addAttribute("nowPage", page);
            model.addAttribute("totalPageBlock", totalPageBlock);
            model.addAttribute("nowPageBlock", nowPageBlock);
            model.addAttribute("startPage", startPage);
            model.addAttribute("endPage", endPage);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return "board/search";
    }

    // 13. 전역 예외 처리
    @ExceptionHandler({RuntimeException.class})
    public String error(HttpServletRequest request, Exception ex, Model model) {
        model.addAttribute("exception", ex);
        model.addAttribute("stackTrace", ex.getStackTrace());
        model.addAttribute("url", request.getRequestURI());
        return "error/runtime";
    }
}