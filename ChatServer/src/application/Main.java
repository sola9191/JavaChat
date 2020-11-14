package application;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;


public class Main extends Application {
	
	public static ExecutorService threadPool;
	//ExecutorService--> 여러개의 쓰레드를 효율적으로 관리하기 위해 사용됨
	//한정된 자원을(쓰레드숫자제한) 이용해서 안정적으로 서버를 운영하기위해 사용 
	public static Vector<Client> clients = new Vector<Client>();	//Vector--> 쉽게 사용할수 있는 형태의 배열
	ServerSocket serverSocket; //서버소켓
	
	public void startServer(String IP, int port) { //서버를 구동시켜서 클라이언트의 연결을 기다리는 메서드
		try {
			serverSocket = new ServerSocket(); //객체생성
			serverSocket.bind(new InetSocketAddress(IP, port)); //IP와 port번호로 클라이언트를 기다림
			
		}catch(Exception e ) {
			if(!serverSocket.isClosed()) { //서버소켓이 닫혀있는 상태아니면
				stopServer(); //서버종료
			}
			return;
		}
		Runnable thread = new Runnable(){ //클라이언트가 접속할때까지 계속 기다리는 쓰레드

			@Override
			public void run() {
				while(true) {
					try {
						Socket socket = serverSocket.accept();
						clients.add(new Client(socket));
						System.out.println("[클라이언트 접속: "
								+ socket.getRemoteSocketAddress()
								+ ": " + Thread.currentThread().getName());
					}catch(Exception e) {
						if(!serverSocket.isClosed()) {
							stopServer();
						}
						break;
					}
				}//end while
			}
		};//end run
		threadPool = Executors.newCachedThreadPool(); //threadpool 초기화
		threadPool.submit(thread); // 클라이언트를 기다리는 쓰레드를 넣어줌
	}
	public void stopServer() { //섭서의 작동을 중지시키는 메소드
		try { 
			Iterator<Client> iterator = clients.iterator();
			while(iterator.hasNext()) { //현재 작동중인 모든 소켓 닫기
				Client client = iterator.next();
				client.socket.close(); //클라이언트의 소켓닫음
				iterator.remove();
			}
			if(serverSocket!=null && !serverSocket.isClosed()) { //서버소켓객체닫기
				serverSocket.close();
			}
			if(threadPool != null & !threadPool.isShutdown()){  //스레드풀 종료
				threadPool.shutdown(); 
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void start(Stage primaryStage) { //UI 생성하고 프로그램 동작시키는 메서드
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(5));
		
		TextArea textArea = new TextArea();
		textArea.setEditable(false); //수정못하게
		textArea.setFont(new Font("고딕", 15));
		root.setCenter(textArea); //textArea 담기
				
		Button toggleButton = new Button("시작하기");
		toggleButton.setMaxWidth(Double.MAX_VALUE);
		BorderPane.setMargin(toggleButton, new Insets(1,0,0,0));
		root.setBottom(toggleButton); //버튼담기
		
		String IP = "127.0.0.1";
		int port = 9876;
		
		toggleButton.setOnAction(event -> {
			if(toggleButton.getText().equals("시작하기")) {
				startServer(IP, port);
				Platform.runLater(() -> { //버튼을 눌렀을ㄹ때 runLater를 사용해서 GUI 요소를 출력해야함
					String message = String.format("[서버시작]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("종료하기");				
				});
			} else {
				stopServer();
				Platform.runLater(() -> { //버튼을 눌렀을때 runLater를 사용해서 GUI 요소를 출력해야함
					String message = String.format("[서버종료]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("시작하기");		
				});
			}
		});
		Scene scene = new Scene(root, 400,400);
		primaryStage.setTitle("[채팅서버]");
		primaryStage.setOnCloseRequest(event -> stopServer());
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
}
