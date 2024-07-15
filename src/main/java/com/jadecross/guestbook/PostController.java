package com.jadecross.guestbook;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class PostController {
	
	@Autowired
	private PostService postService;
	
	private int requestCount = 0;
	private int healthCheckCount = 0;

	@GetMapping("/")
	public String index(Model model
						, @RequestHeader(value="User-Agent") String userAgent
						, HttpServletRequest request) {
		request.getSession();
		
		// 01. 방명록 조회
		model.addAttribute("posts", postService.getAll());
		model.addAttribute("host", new Host());
		model.addAttribute("userAgent", userAgent);
		
		// 02. 의미없이 CPU 사용량 증가
		this.useCPU(1000);
		
		return "index";
	}
	
	/**
	 * 게시글을 json으로 리턴
	 * @return
	 */
	@CrossOrigin
	@GetMapping("/posts")
	public @ResponseBody List<Post> posts() {
		//
		return postService.getAll();
	}
	

	@PostMapping("/")
	public String insertPost(@ModelAttribute Post post
							, Model model
							, @RequestHeader(value="User-Agent") String userAgent) {
		// 01-1. 파일첨부
		if(post.getUploadingFile().getOriginalFilename().equals("") == false) {
			String uploadedFile = this.uploadFile(post.getUploadingFile(), post.getName());
			post.setAttachedFile(uploadedFile);
		}
		
		// 01-2. DB에 저장
		post.setWriteDate((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date()));
		postService.add(post);
		
		System.out.println("===> 방명록 저장");
		System.out.println(post);

		// 02. 방명록 조회
		model.addAttribute("posts", postService.getAll());
		model.addAttribute("host", new Host());
		model.addAttribute("userAgent", userAgent);

		return "index";
	}
	
	@GetMapping("/sleep")
	public String sleep(Model model
						,@RequestHeader(value="User-Agent") String userAgent
						,@RequestParam(value = "sec")String sleepSec) {
		// 방명록 조회
		model.addAttribute("posts", postService.getAll());
		model.addAttribute("host", new Host());
		model.addAttribute("userAgent", userAgent);
		
		// 의도적으로 응답시간 지연
		try {
			Thread.sleep(Integer.parseInt(sleepSec) * 1000);
		} catch (Exception e) {  }
	
		return "index";
	}
	
	@GetMapping("/healthcheck")
	public String healthCheck(Model model
							, @RequestHeader(value="User-Agent") String userAgent) {
		model.addAttribute("host", new Host());
		model.addAttribute("userAgent", userAgent);

	    return "healthcheck";
	}
	
	@GetMapping("/healthcheck30PercentFail")
	public String healthcheck30PercentFail(Model model
							, @RequestHeader(value="User-Agent") String userAgent) {
		model.addAttribute("host", new Host());
		model.addAttribute("userAgent", userAgent);
		
		// 끝자리가 7, 8, 9 이면 500 Internal Server Error 발생
		healthCheckCount++;
		int lastDigit = getLastDigit(healthCheckCount);
		if(lastDigit == 7 || lastDigit == 8 || lastDigit == 9)  throw new RuntimeException();
	    
	    return "healthcheck";
	}
	
	private int getLastDigit(int num) {
		String temp = num + "";
		return Integer.parseInt(temp.charAt(temp.length()-1)+"");
	}
	
	@GetMapping("/downloadFile/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) throws Exception{
		
		File downloadFile = new File(System.getProperty("user.dir") + "/upload/" + fileName);
		Resource resource = new UrlResource(downloadFile.toURI());
		String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
		
		if(contentType == null) contentType = "text/plain";

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
				.body(resource);
    }
	
	/**
	 * 현재 Working Directory/upload 폴더에 파일을 첨부
	 * @param file
	 * @param uploaderName
	 * @return 업로드한 파일명
	 */
	private String uploadFile(MultipartFile file, String uploaderName) {
		String currentWorkingDirectory = System.getProperty("user.dir");
		String originalFilename = file.getOriginalFilename(); // 클라이언트 시스템의 FullPath 포함한 파일명
		String uploadFileName = this.getCurrentTimeMillisFormat() + "_" + uploaderName + "_" + FilenameUtils.getName(originalFilename);
		
		File uploadFile = new File( currentWorkingDirectory + "/upload/" + uploadFileName);
		
		try {
			uploadFile.getParentFile().mkdirs(); // upload 디렉토리가 없으면 생성
			file.transferTo(uploadFile);
			
			// S3 File Upload - aws configure 가 선행되어야 한다.
			String s3BucketID = System.getenv("S3BUCKET_ID");
			if(s3BucketID != null ){
				String s3CpCommand = "aws s3 cp /app/upload/[ATTACHED_FILE] s3://[BUCKET_ID]/files/";
				s3CpCommand = s3CpCommand.replace("[ATTACHED_FILE]", uploadFileName);
				s3CpCommand = s3CpCommand.replace("[BUCKET_ID]", s3BucketID);
				System.out.println("s3CpCommand=" + s3CpCommand);
				
				new ProcessBuilder("/bin/bash", "-c", s3CpCommand).start();
			}
		} catch (Exception ex) { } 
		
		return uploadFileName;
	}
	
	/**
	 * 현재시간을 "년월일시분초밀리초" 로 반환
	 * @return
	 */
	private String getCurrentTimeMillisFormat() {
		long currentTime = System.currentTimeMillis(); 
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS"); 
		return dateFormat.format(new Date(currentTime)); 
	}
	
	/**
	 * CPU 사용률을 높이기 위해 LOOP를 돌면서 산술연산(double 연산) 수행
	 * @param loopCount
	 * @return
	 */
	private double useCPU(int loopCount) {
		double result = 0;
		for (int i = 1; i < loopCount; i++) {
			result = i * Math.random() / loopCount;
			for (int j = 1; j < loopCount; j++) {
				result = i * j * Math.random() / loopCount;

				for (int k = 1; k < loopCount; k++) {
					// CPU 만 소모
				}
			}
		}
		return result;
	}
}