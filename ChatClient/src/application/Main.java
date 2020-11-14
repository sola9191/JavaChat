package application;
	
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;


public class Main extends Application {
	
	Socket socket;
	TextArea textArea; //메세지가 출력되는 공간
	
	//클리이언트 프로그램 동작 메서드
	public void startClient(String IP, int port) {
		Thread thread = new Thread() { //Threadpool이 필요없어서 그냥 thread객체 한개생성
			public void run() {
				try {
					socket = new Socket(IP, port); 
					recieve(); //서버로부터 메세지 받기위함
				}catch(Exception e) {
					if(!socket.isClosed()) {
						stopClient();
						System.out.println("[서버접속실패]");
						Platform.exit(); //프로그램자체를 종료
					}
				}
			}
		};
		thread.start();
	}
	//클라이언트 프로그램 종료 메서드
	public void stopClient() {
		try {
			if(socket !=null && !socket.isClosed()) {
				socket.close(); //자원 해제
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	//서버로부터 메세지를 전달받는 메서드
	public void recieve() {
		while(true) { //계속 전달받기위해 무한루프
			try {
				InputStream in = socket.getInputStream();
				byte [] buffer = new byte[512];
				int length = in.read(buffer);
				if(length == -1) throw new IOException();
				String message = new String (buffer, 0, length, "UTF-8");
				Platform.runLater(()->{
					textArea.appendText(message);
				});
				
			}catch(Exception e) {
				stopClient();
				break;
			}
		}
		
	}
	//서버로 메세지를 전송하는 메서드
	public void send (String message) {
		Thread thread = new Thread() {
			public void run() {
				try {
					OutputStream out = socket.getOutputStream();
					byte [] buffer = message.getBytes("UTF-8"); //utf-8로 인코딩해서보내기
					out.write(buffer);
					out.flush(); //메세지전송의 끝을 알림
				}catch(Exception e) {
					stopClient();
				}
			}
		};
		thread.start();
	}
	//실제로 프로그램을 동작시키는 메서드 
	@Override
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(5));
		
		HBox hbox = new HBox();
		hbox.setSpacing(5);
		 
		TextField userName = new TextField();
		userName.setPrefWidth(150); //너비
		userName.setPromptText("닉네임을 입력하세요"); //메세지 보이게
		HBox.setHgrow(userName, Priority.ALWAYS); //Hbox내부에서 해당 textField가 출력될수있도록 만듬
		
		TextField IPText = new TextField("127.0.0.1"); //서버의 IP주소
		TextField portText = new TextField("9876"); //
		portText.setPrefWidth(80);
		
		hbox.getChildren().addAll(userName, IPText, portText); //hbox의 3개의 textField가 추가되도록 만듬
		root.setTop(hbox); // hbox를 위쪽에 달기
		
		textArea = new TextArea(); //textArea 초기화
		textArea.setEditable(false); //내용수정못하게
		root.setCenter(textArea); //layout중간에 textArea보이게
		
		TextField input = new TextField();
		input.setPrefWidth(Double.MAX_VALUE);
		input.setDisable(true);
		
		//접속하기 이전에는 메세지 못보내게
		input.setOnAction(event->{ //엔터버튼 눌렀을떄
			send(userName.getText() + ": " + input.getText() + "\n"); // 이벤트 발생시 서버로 사용자이름과 메세지 전달함
			input.setText(""); //전송후 메세지 전송칸비우기
			input.requestFocus();
			
		});
		Button sendButton = new Button("보내기");
		sendButton.setDisable(true); //보내기 번튼 비활성화
		sendButton.setOnAction(event->{ //버튼 눌렀을때
			send(userName.getText() + ": " + input.getText() + "\n");
			input.setText("");
			input.requestFocus();
		});
		
		Button connectionButton = new Button("접속하기"); //서버와 연결해서 접속
		connectionButton.setOnAction(event->{
			if(connectionButton.getText().equals("접속하기")) {
				int port = 9876;
				try {
					port = Integer.parseInt(portText.getText()); //사용자가 입력한 포트번호를 int로 형변환
				}catch(Exception e) {
					e.printStackTrace();
				}
				startClient(IPText.getText(), port); //사용자가 별도의 포트번호입력하면 그번호로 접속이루어지게
				Platform.runLater(() ->{
					textArea.appendText("[채팅방접속]\n"); //이문구가 출력됨
				});
				connectionButton.setText("종료하기"); //접속후 버튼 이름바꿔주기
				input.setDisable(false); //사용자가 입력가능하게함
				sendButton.setDisable(false); //버튼 활성화시켜서 메세지 전송가능하게
				input.requestFocus(); //포커싱주기
			}else { //종료하기버튼이였따면
				stopClient(); //클라이언트 종료
				Platform.runLater(() ->{
					textArea.appendText("[채팅방퇴장]\n");
				});
				connectionButton.setText("접속하기"); //종료후 버튼이름바꿔주기
				input.setDisable(true);
				sendButton.setDisable(true);
			}
		});
		
		BorderPane pane = new BorderPane();
		pane.setLeft(connectionButton); //왼쪽에 접속하기버튼
		pane.setCenter(input);
		pane.setRight(sendButton);
		
		root.setBottom(pane); //아래쪽에 이pane들어가게
		Scene scene = new Scene(root, 400, 400);
		primaryStage.setTitle("[채팅 클라이언트]");
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(event-> stopClient()); //사용자가 화면닫기하면 stop client
		primaryStage.show(); //setting 다 끝나고 보여지도록
		
		connectionButton.requestFocus(); // 프로그램실행되면 접속하기 버튼 포커싱되게 만들기
	}
	//프로그램 진입점
	public static void main(String[] args) {
		launch(args);
	}
}
