export type ApiError = {
  code: string;
  message: string;
  traceId: string;
  timestamp: string;
};

export type UserResponse = {
  id: number;
  username: string;
  phone: string;
  nickname: string;
  createdAt: string;
  updatedAt: string;
};

export type PointBalanceResponse = {
  userId: number;
  balance: number;
  accountCreated: boolean;
  updatedAt: string | null;
};

export type PointTransactionResponse = {
  transactionId: number;
  businessId: number;
  userId: number;
  type: 'CREDIT' | 'DEBIT';
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  source: string;
  remark: string | null;
  createdAt: string;
  replayed: boolean;
};

export type PointTransactionPageResponse = {
  items: PointTransactionResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
