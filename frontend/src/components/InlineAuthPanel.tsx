import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { apiClient } from '../lib/api/client';
import { useAuth } from '../lib/auth/AuthContext';
import { SectionCard } from './SectionCard';

export function InlineAuthPanel({ title, subtitle }: { title: string; subtitle: string }) {
  const { login } = useAuth();
  const [credentials, setCredentials] = useState({ username: 'trader', password: 'trader123!' });
  const [message, setMessage] = useState(subtitle);

  const loginMutation = useMutation({
    mutationFn: () => apiClient.login(credentials.username, credentials.password),
    onSuccess: (payload) => {
      login(payload);
      setMessage(`${payload.role} 세션이 활성화되었습니다.`);
    },
    onError: (error: Error) => setMessage(error.message),
  });

  return (
    <SectionCard title={title} subtitle={subtitle}>
      <form
        className="auth-form"
        onSubmit={(event) => {
          event.preventDefault();
          loginMutation.mutate();
        }}
      >
        <input
          value={credentials.username}
          onChange={(event) => setCredentials((current) => ({ ...current, username: event.target.value }))}
          placeholder="아이디"
        />
        <input
          type="password"
          value={credentials.password}
          onChange={(event) => setCredentials((current) => ({ ...current, password: event.target.value }))}
          placeholder="비밀번호"
        />
        <button className="primary-button" type="submit" disabled={loginMutation.isPending}>
          로그인
        </button>
      </form>
      <p className="muted">{message}</p>
    </SectionCard>
  );
}
