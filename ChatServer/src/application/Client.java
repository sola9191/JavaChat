package application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
	//한명의 클라이언트와 통신하기 위해 필요한 기능
	
	Socket socket;
	
	public Client(Socket socket) { //생성자
		this.socket = socket;
		receive();
	}

	public void receive() {		 //클라이언트로부터 메세지 전달받는메서드
		Runnable thread = new Runnable() {
			@Override
			public void run() { 	//하나의 쓰레드가 어떤모듈로 동작할건지 여기서 정의
				try {
					while(true) {
						InputStream in = socket.getInputStream(); 
						byte [] buffer = new byte[512]; //한번에 512byte 전달받을수있게
						int legnth = in.read(buffer); //클라이언트에게 받은것을 buffer에 저장
						while(legnth == -1) throw new IOException(); //오류발생시 알려주기
						System.out.println("[메세지수신성공]"	
						+ socket.getRemoteSocketAddress() //접속한 클라이언트의 IP주소정보
						+ ": " + Thread.currentThread().getName()); //접속한 클라이언트의 쓰레드의 이름값 출력
						String message = new String(buffer, 0, legnth, "UTF-8"); //buffer index 0부터 끝까지 인코딩
						for(Client client : Main.clients) { //전달받은 메세지를 다른클라이언트들에게도
							client.send(message);
						}						
					}
				}catch (Exception e) { e.printStackTrace(); 
					try {
						System.out.println("[메세지수신오류]"
								+ socket.getRemoteSocketAddress()
								+": " +Thread.currentThread().getName());					
					}catch(Exception e2) {
						e.printStackTrace();
					}
				}
			}			
		};
		Main.threadPool.submit(thread); //쓰레드풀에 쓰레드를 등록
		
	}
	
	public void send(String message) { //클라이언트에게 메세지 전송하는 메서드
		Runnable thread = new Runnable() {

			@Override
			public void run() {
				try {
					OutputStream out = socket.getOutputStream();
					byte [] buffer = message.getBytes("UTF-8"); 
					out.write(buffer);
					out.flush(); // 꼭 써줘야함
				}catch(Exception e) { e.printStackTrace();
					try {
						System.out.println("[메시지 송신 오류]"
								+ socket.getRemoteSocketAddress()
								+ ": " + Thread.currentThread().getName());
						Main.clients.remove(Client.this); //오류생긴 클라이언트 배열에서 제거
						socket.close();
					}catch(Exception e2) {
						e2.printStackTrace();
					}
				}
			}
		};
		Main.threadPool.submit(thread); //쓰레드풀에 쓰레드를 등록
	}	
}
