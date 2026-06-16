export async function getHealth() {
  const res = await fetch('/api/health')
  if (!res.ok) throw new Error('API failed')
  return res.json()
}