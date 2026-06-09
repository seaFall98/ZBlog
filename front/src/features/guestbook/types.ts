export type GuestbookMessageView = {
  id: string;
  name: string;
  content: string;
  date: string;
  avatar: string;
};

export type GuestbookSubmitResult = {
  id: string;
  status: string;
  message: string;
};
